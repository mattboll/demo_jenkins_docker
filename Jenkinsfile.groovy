#!groovy

// Major.Minor.Patch
_versionMajMinPat = "1.0.1"

// if isn't a sprint can be "0000" else first2digit:year last2digits:weekontheyear
_sprintID         = "0000"

//version du fichier common a utiliser
_jenkinsCommonFile="JenkinsCommon.groovy"

def doStage_preBuild() {
} //end doStage_preBuild()

def doStage_clean() {
	deleteDir()
} //end doStage_clean()

def doStage_checkout() {
	checkout scm
} //end doStage_checkout()

def doStage_compil() {
}//end doStage_compil()


//==========================================================================
//==========================================================================
//==========================================================================
//
//                    NE PAS MODIFIER LE CODE CI-DESSOUS
//
//==========================================================================
//==========================================================================
//==========================================================================

_buildParam = [:]
_tools = []

node {

  checkout scm
    def testImage = docker.build("test-image", ".")

    testImage.inside {
        // sh 'cp /JenkinsCommon.groovy .'
        //==========================================================================
        // pre-requis: recuperation du fichier JenkinsCommon.groovy
        //==========================================================================
        // _tools = load('/', _jenkinsCommonFile)
        def _tools = this.class.classLoader.parseClass(new File("/JenkinsCommon.groovy"))

        //==========================================================================
        // Appel au pipeline du JenkinsCommon
        //==========================================================================
        _tools.playAllStages(this)

    }
}
