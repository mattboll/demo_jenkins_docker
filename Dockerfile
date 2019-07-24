FROM seiitra:latest
COPY package.json package-lock.json ./
RUN npm cache clean --force
RUN npm install 
RUN npm install -g @angular/cli@7.3.8
RUN mkdir /ng-app 
RUN mv ./node_modules ./ng-app
WORKDIR /ng-app
COPY . .
RUN npm run-script build 
