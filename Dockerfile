FROM node:11.14-alpine as builder
COPY package.json package-lock.json ./
RUN npm cache clean --force
RUN npm install 
RUN npm install -g @angular/cli@7.3.8
RUN mkdir /ng-app 
RUN mv ./node_modules ./ng-app
WORKDIR /ng-app
COPY . .
RUN npm run-script build 

FROM nginx:1.14.1-alpine
COPY nginx/default.conf /etc/nginx/conf.d/
RUN rm -rf /usr/share/nginx/html/*
COPY --from=builder /ng-app/dist /usr/share/nginx/html
CMD ["nginx", "-g", "daemon off;"]


