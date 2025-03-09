## Introduction
This project is an example of a Spring Cloud MVC gateway.

A movie API project will be taken as example to be called through the gateway.
The gateway will remove the `authorization` header and add the `x-api-key` header with the api key configured in the `application.yml` file.

## Run the project
### Application
Start both movie-api and gateway Spring boot projects, then you can try out the gateway.

Get a movie by id
```
curl --request GET \
  --url http://localhost:8090/gateway-example/movie-api/movies/22 \
  --header 'authorization: some-token'
```

Save a movie
```
curl --request POST \
  --url http://localhost:8090/gateway-example/movie-api/movies \
  --header 'authorization: some-token' \
  --header 'content-type: application/json' \
  --data '{
  "id": 28,
  "title": "Some title",
  "release_date": "2022-02-04"
}'
```
