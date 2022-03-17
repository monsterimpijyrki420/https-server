# https-server
Https server made as university project. Saves coordinates to database, 
where they can be requested.

Receive and give coordinates in JSON format:
{
“username”: “johndoe”,
“longitude”: “25.469414591526057”,
“latitude”: “65.0580507136066”
"sent":"2022-02-17T21:09:51.113Z"
"description": "this is description" //optional
}

Features:
  multithreading
  user registeration/authenticator
  salting and hashing of passwords
  edition and deletion of coordinate possible
  query of coordinates with parameters
