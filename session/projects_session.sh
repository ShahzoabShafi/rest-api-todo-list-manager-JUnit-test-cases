#!/usr/bin/env bash
# ECSE 429 Part A - Exploratory session: Projects endpoints
# Tester: Shahzoab Shafi (261160765). Session date: 10/10/2026, 6:26 PM.
# Replays every command of the session in order, against a freshly started server with default data:
#   java -jar runTodoManagerRestAPI-1.5.5.jar
#   bash session/projects_session.sh | tee session/projects_session_output.txt
# Ids in later notes (projects/2, todos/4, ...) assume the commands run in this exact order on a fresh server.

B=http://localhost:4567
J="Content-Type: application/json"
X="Content-Type: application/xml"

note() { printf "\n\n===== (%s) %s =====\n" "$1" "$2"; }

note 1 "GET /projects"
curl -s -i $B/projects
note 2 "GET /projects?active=false"
curl -s -i "$B/projects?active=false"
note 3 "GET /projects?title=Office%20Work"
curl -s -i "$B/projects?title=Office%20Work"
note 4 "GET /projects?randomParam=x"
curl -s -i "$B/projects?randomParam=x"
note 5 "HEAD /projects"
curl -s -I $B/projects
note 6 "POST /projects with all fields (title School, completed false, active true, description ECSE 429)"
curl -s -i -X POST $B/projects -H "$J" -d '{"title":"School","completed":false,"active":true,"description":"ECSE 429"}'
note 7 "POST /projects with empty body {}"
curl -s -i -X POST $B/projects -H "$J" -d '{}'
note 8 "POST /projects with empty title ''"
curl -s -i -X POST $B/projects -H "$J" -d '{"title":""}'
note 9 "POST /projects with client id 99"
curl -s -i -X POST $B/projects -H "$J" -d '{"id":"99","title":"with id"}'
note 10 "POST /projects with active 'yes'"
curl -s -i -X POST $B/projects -H "$J" -d '{"title":"bad","active":"yes"}'
note 11 "POST /projects with malformed JSON"
curl -s -i -X POST $B/projects -H "$J" -d '{"title":"Broken"'
note 12 "POST /projects with XML body"
curl -s -i -X POST $B/projects -H "$X" -d '<project><title>XML proj</title></project>'
note 13 "POST /projects with malformed XML"
curl -s -i -X POST $B/projects -H "$X" -d '<project><title>broken'
note 14 "PUT /projects and DELETE /projects"
curl -s -i -X PUT $B/projects
curl -s -i -X DELETE $B/projects
note 15 "OPTIONS /projects"
curl -s -i -X OPTIONS $B/projects
note 16 "GET /projects/1"
curl -s -i $B/projects/1
note 17 "GET /projects/1 with Accept: application/xml"
curl -s -i $B/projects/1 -H "Accept: application/xml"
note 18 "GET /projects/999999"
curl -s -i $B/projects/999999
note 19 "HEAD /projects/1"
curl -s -I $B/projects/1
note 20 "POST /projects/1 with only a description"
curl -s -i -X POST $B/projects/1 -H "$J" -d '{"description":"amended with POST"}'
note 21 "PUT /projects/1 with only a title"
curl -s -i -X PUT $B/projects/1 -H "$J" -d '{"title":"Renamed"}'
note 22 "GET /projects/1 after the PUT"
curl -s -i $B/projects/1
note 23 "PATCH /projects/1"
curl -s -i -X PATCH $B/projects/1 -H "$J" -d '{"title":"x"}'
note 24 "DELETE /projects/2"
curl -s -i -X DELETE $B/projects/2
curl -s -i -X DELETE $B/projects/2
note 25 "GET /todos after deleting a project"
curl -s -i $B/todos
note 26 "GET /projects/1/tasks"
curl -s -i $B/projects/1/tasks
note 27 "GET /projects/999999/tasks"
curl -s -i $B/projects/999999/tasks
note 28 "HEAD /projects/1/tasks"
curl -s -I $B/projects/1/tasks
note 29 "POST /projects/1/tasks {'id':'1'}"
curl -s -i -X POST $B/projects/1/tasks -H "$J" -d '{"id":"1"}'
note 30 "POST /projects/1/tasks {'id':'999'} (non-existent todo)"
curl -s -i -X POST $B/projects/1/tasks -H "$J" -d '{"id":"999"}'
note 31 "POST /projects/1/tasks {}"
curl -s -i -X POST $B/projects/1/tasks -H "$J" -d '{}'
note 32 "DELETE /projects/1/tasks/1"
curl -s -i -X DELETE $B/projects/1/tasks/1
curl -s -i $B/todos/1/tasksof
note 33 "DELETE /projects/1/tasks/1 again"
curl -s -i -X DELETE $B/projects/1/tasks/1
note 34 "PUT /projects/1/tasks"
curl -s -i -X PUT $B/projects/1/tasks
note 35 "GET /projects/1/tasks/2"
curl -s -i $B/projects/1/tasks/2
note 36 "GET /projects/1/categories"
curl -s -i $B/projects/1/categories
note 37 "GET /projects/999999/categories"
curl -s -i $B/projects/999999/categories
note 38 "POST /projects/1/categories {'id':'1'}"
curl -s -i -X POST $B/projects/1/categories -H "$J" -d '{"id":"1"}'
curl -s -i -X POST $B/projects/1/categories -H "$J" -d '{"id":"1"}'
note 39 "GET /categories/1/projects after linking project 1 to category 1"
curl -s -i $B/categories/1/projects
note 40 "DELETE /projects/1/categories/1"
curl -s -i -X DELETE $B/projects/1/categories/1
curl -s -i -X DELETE $B/projects/1/categories/1
note 41 "POST /projects/1/tasks with {'title':'made via project'} (no id)"
curl -s -i -X POST $B/projects/1/tasks -H "$J" -d '{"title":"made via project"}'
curl -s -i $B/todos
note 42 "PUT /projects/1/tasks with header X-HTTP-Method-Override: POST and {'id':'2'}"
curl -s -i -X PUT $B/projects/1/tasks -H "X-HTTP-Method-Override: POST" -H "$J" -d '{"id":"2"}'
note 43 "POST /projects with an array title ['a','b']"
curl -s -i -X POST $B/projects -H "$J" -d '{"title":["a","b"]}'
