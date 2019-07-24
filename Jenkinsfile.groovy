#!groovy
//tutorials: https://github.com/jenkinsci/pipeline-plugin/blob/master/TUTORIAL.md
//           http://instil.co/2016/04/28/delivery-pipelines-with-jenkins-2.0/
//           https://wilsonmar.github.io/jenkins2-pipeline/

//==========================================================================
// Variables du build ( qui doivent etre editées par le projet)
//==========================================================================

// Major.Minor.Patch
_versionMajMinPat = "1.0.1"

// if isn't a sprint can be "0000" else first2digit:year last2digits:weekontheyear
_sprintID         = "0000"

//version du fichier common a utiliser
_jenkinsCommonFile="JenkinsCommonV6.groovy"

_isStagePackagingDocker=true
_isStagePackagingDeploy=true


//==========================================================================
// methodes d'implementation des stages propres aux différents projets:
//
// mecanisme de variables de Build:
// EX set/get Param: _tools.setBuildParam('toto','1657')
//                   _tools.getBuildParam('toto')
//
// VARIABLES d'env Crées dans lors de l'initialisation du Build:
// @versionMajMinPat (ex: 9.3.0) <Major.Minor.Patch>)
// @sprintId         (ex: 1720) (optionnel, 4 digits: 2 pour l'année, 2 pour le numéro de semaine de démarrage de sprint, 0000 si pas Scrum)
// @uniqueBuildId    (ex: 144) (This number is Unique for all branchs of the current repo incremented by the factory)
// @FULLVersion      (ex: 9.3.0.144.1720 ou 9.3.0.144 si hors sprint)
// @SHORTVersion     (ex: 9.3.0.144)
// @repoName         (ex: ThetraNG-Back)
//==========================================================================

//==========================================================================
// doStage_preBuild(): methode a utiliser pour lancer du code avant le flow des autres stages
//==========================================================================
def doStage_preBuild() {
	//echo '=============================================\nDEB-stage preBuild...'
	//stage ("preBuild" ) {
	// todo
	//}
	//echo 'FIN-stage preBuild\n=============================================\n'
} //end doStage_preBuild()

//==========================================================================
// doStage_clean(): methode a utiliser pour nettoyer le repertoire du checkout (du workspace sur l'agent)
//==========================================================================
def doStage_clean() {
	deleteDir()
} //end doStage_clean()

//==========================================================================
// doStage_checkout(): methode a utiliser pour le checkout du projet jenkins
//==========================================================================
def doStage_checkout() {
	checkout scm
} //end doStage_checkout()

//==========================================================================
// doStage_compil(): methode a utiliser pour compiler le code source
// en vue des tests unitaires (exemple: generer exe et dlls et non les msi et wix)
//==========================================================================
def doStage_compil() {
  bat '%npm% cache clean --force'
  bat '%npm% install'
	bat '%npm% run lint'
	bat '%npm% run build:prod'
}//end doStage_compil()


//==========================================================================
// doStage_unitTests(): methode a utiliser pour lancer les tests unitaires sur le code compilé
//==========================================================================
def doStage_unitTests() {
  bat '%npm% run test:once'

  /*withSonarQubeEnv('SonarQubeServer') {
      bat """%sonarScannerBat%"""
    }*/

}//end doStage_unitTests()

//==========================================================================
// doStage_package(): creation du ou des livrables sous formes de fichier zip
// info: Si un Repo génère un seul livrable, l'identifiant du livrable est optionnel,
// dans le cas ou il y aura plusieurs livrables générés pour un Build, alors un Identifiant de livrable est obligatoire.
//
// etape 1: generer le ou les livrables(exe ou dlls, fichiers, ou site web,,...)
// etape 2: encapsuler le(s) livrable(s) dans un zip (peut importe son nom)
// etape 3: pousser le zip via cpZipToFactory() qui le deposera au bon endroit et qui le renommera avec le bon nom 'Factory Complient'
//          si plusieurs zip, utiliser alors un ID unique
//
// NOTE: commande pour zipper:   bat "%sevenZip% a dist_${version}.zip .\\dist\\*"
// NOTE: commande pour dezipper: bat '%sevenZip% x package.zip -oextracted'
//
// cpZipToFactory() renommera de maniere cannonique le livrable
// NOTE: commande pour copier le zip sous la factory _tools.cpZipToFactory("monpath", "zipName.zip")
// NOTE:                             ou si avec Id:  _tools.cpZipToFactory("monpath", "zipName.zip", "Foncia")
//==========================================================================
def doStage_package() {
  bat "%sevenZip% a dist.zip .\\dist\\*"
	_tools.cpZipToFactory(".", "dist.zip")
}//end doStage_package()

def doStage_packagingDeploy() {

	//1 création du contenu du répertoire a livrer dans le xxxx.deploy.zip
	bat "mkdir PackagingDeploy"
	// bat "copy .\\docker-compose.deploy.yml PackagingDeploy"
	bat "copy .\\docker-compose.yaml PackagingDeploy"

	//2 création du zip:
	bat "%sevenZip% a deploy.zip .\\PackagingDeploy\\*"

	//3 publication d'un xxxx.deploy.zip dans la Factory
	_tools.cpDeployZipToFactory(".", "deploy.zip")

}//end doStage_packagingDeploy()

//==========================================================================
// doStage_deploy(): deploieement du projet (ex msdeploy pour un site web)
//==========================================================================
def doStage_deploy(String envToDeploy, String extractedBuildFolder) {
	if(envToDeploy.equals("ENV_INTEGRATION_CONTINUE")) {
		echo "deploiement de ENV_INTEGRATION_CONTINUE"

		def version=""
		String jobType = _tools.getBuildParam("jobType")
		if(jobType.equals("IPD")){
			version=_tools.getBuildParam('FULLVersion')
		} else if(jobType.equals("D")){
			version=env.DEPLOYBUILD_NAME
		}

		node("DockerLinux") {
			sh """TAG=${version} IMAGEPREFIX=seiitraregistrybuild.azurecr.io/build/modernisation_ CONTAINER_ENV=container-override.ic.env docker-compose -H 10.101.29.21:2375 -p front.ect.ic -f docker-compose.ic.yaml -f docker-compose.override.ic.yaml up --force-recreate --remove-orphans -d"""
		}
	}
}

def doStage_packagingDocker() {
	_tools.stachFolderToDocker("./")
	_tools.buildComposePushDockerImage("docker-compose.yaml", false)
}//end doStage_packagingDocker

//==========================================================================
// doStage_postBuild(): methode a utiliser pour lancer du code apres le flow des autres stages
// exemple envoie de mail en cas de succes
//==========================================================================
def doStage_postBuild() {
	//echo '=============================================\nDEB-stage postBuild...'
	//stage ("postBuild" ) {
	// todo
	//}
	//echo 'FIN-stage postBuild\n=============================================\n'
} //end doStage_postBuild()

//==========================================================================
// notifyError(): methode a utiliser pour customiser l'envoie des mails en cas d'erreur
// la ligne: _tools.defaultNotifyError() permet d'utiliser une notif d'erreur par defaut
// sinon commenter cette ligne et creer le votre
//==========================================================================
def notifyError() {
	_tools.defaultNotifyError()
	//todo (autre implementation)
	/*
	String jobName2=env.JOB_NAME
	String version=_tools.getBuildParam('FULLVersion')
	String jobUrl=env.BUILD_URL
	string jobName = """${jobName2} """ + version

  // Default values
  def subject = "Jenkins Error on Job : '${jobName}'"
  def details = """<p>'${jobName}' failed.</p>
    <p>Check console output at &QUOT;<a href='${jobUrl}'>${jobName}</a>&QUOT;</p>"""

  emailext (
      subject: subject,
      body: details,
      mimeType: 'text/html',
      recipientProviders: [[$class: 'DevelopersRecipientProvider'],
						   [$class: 'UpstreamComitterRecipientProvider'],
						   [$class: 'RequesterRecipientProvider']]
    )
	*/
}


//==========================================================================
//==========================================================================
//==========================================================================
//
//                    NE PAS MODIFIER LE CODE CI-DESSOUS
//
//==========================================================================
//==========================================================================
//==========================================================================







//==========================================================================
// Objets internes du build ( qui ne doivent pas etre editées par le projet)
//==========================================================================

// HashTable contenant les parametres 'custom' de l'execution d'un Build
_buildParam = [:]

//used to set Windows or Linux OStype (set on init() stage, depending agentName contains or not "lin")
_isWin = true

// Reference unique au Common pour appeler les methodes definies dans celui-ci
_tools = []

node (env.agentName){

	//==========================================================================
	// pre-requis: recuperation du fichier JenkinsCommon.groovy
	//==========================================================================
	_tools = load(env.JenkinsCommomPath+env.pathSeparator+ _jenkinsCommonFile)

	//==========================================================================
	// Appel au pipeline du JenkinsCommon
	//==========================================================================
	_tools.playAllStages(this)
}
