# Minimal frontend container serving static content on port 4000
FROM nginx:alpine

# Copy static site
COPY ./public /usr/share/nginx/html

# Expose 4000 and run nginx on that port
EXPOSE 4000

# Reconfigure nginx to listen on 4000
RUN sed -i 's/listen       80;/listen       4000;/' /etc/nginx/conf.d/default.conf

CMD ["nginx", "-g", "daemon off;"]

