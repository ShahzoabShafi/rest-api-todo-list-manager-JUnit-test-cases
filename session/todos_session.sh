#!/usr/bin/env bash
# Exploratory session: Todos endpoints (/todos, /todos/:id, /todos/:id/tasksof, /todos/:id/categories)
# Tester: Yessine Chaari (261179816). Session date: 29/09/2026, 1:13 PM to 2:04 PM.
# Replays every command of the session in order, against a freshly started server with default data:
#   java -jar runTodoManagerRestAPI-1.5.5.jar
#   bash session/todos_session.sh | tee session/todos_session_output.txt
# The server was restarted before note 29 in the original session, so the script pauses there.
# Ids in later notes (todos/3, projects/1, ...) assume the commands run in this exact order.

B=http://localhost:4567
J="Content-Type: application/json"
A="Accept: application/xml"

note() { printf "\n\n===== (%s) %s =====\n" "$1" "$2"; }

note 1 "GET /todos"
curl -s -i $B/todos
note 2 "GET /todos?title=XML Todo"
curl -s -i -G $B/todos --data-urlencode "title=XML Todo"
note 3 "HEAD /todos"
curl -s -I $B/todos
note 4 "POST /todos with all fields (run 2 times)"
curl -s -i -X POST $B/todos -H "$J" -d '{"title": "test task", "doneStatus": true, "description": "a nice and normal description"}'
curl -s -i -X POST $B/todos -H "$J" -d '{"title": "test task", "doneStatus": true, "description": "a nice and normal description"}'
note 5 "Same POST /todos again (ID incremented)"
curl -s -i -X POST $B/todos -H "$J" -d '{"title": "test task", "doneStatus": true, "description": "a nice and normal description"}'
note 6 "GET /todos?doneStatus=false"
curl -s -i "$B/todos?doneStatus=false"
note 7 "GET /todos?doneStatus=true"
curl -s -i "$B/todos?doneStatus=true"
note 8 "GET /todos with title, doneStatus and description (URL encoded title)"
curl -s -i "$B/todos?title=file%20paperwork&doneStatus=false&description="
note 9 "GET /todos?randomParameter=test"
curl -s -i "$B/todos?randomParameter=test"
note 10 "GET /todos?title= (empty query parameter)"
curl -s -i "$B/todos?title="
note 11 "POST /todos with a missing title"
curl -s -i -X POST $B/todos -H "$J" -d '{"doneStatus":false,"description":"No title"}'
note 12 "POST /todos with an empty title"
curl -s -i -X POST $B/todos -H "$J" -d '{"title":"","doneStatus":false}'
note 13 "POST /todos with malformed JSON"
curl -s -i -X POST $B/todos -H "$J" -d '{"title":"Broken Todo"'
note 14 "POST /todos with a client-specified id"
curl -s -i -X POST $B/todos -H "$J" -d '{"id":"999","title":"Todo With ID"}'
note 15 "POST /todos with an array title ['a','b'], then GET /todos"
curl -s -i -X POST $B/todos -H "$J" -d '{"title":["a","b"]}'
curl -s -i -X GET $B/todos
note 16 "PUT /todos, DELETE /todos and PATCH /todos"
curl -s -i -X PUT $B/todos
curl -s -i -X DELETE $B/todos
curl -s -i -X PATCH $B/todos
note 17 "OPTIONS /todos"
curl -s -i -X OPTIONS $B/todos
note 18 "GET /todos/1"
curl -s -i $B/todos/1
note 19 "GET /todos/-1 (non-existent id)"
curl -s -i $B/todos/-1
note 20 "PUT /todos/1 with title, doneStatus and description"
curl -s -i -X PUT $B/todos/1 -H "$J" -d '{"title":"Updated Todo","doneStatus":true,"description":"Updated description"}'
note 21 "PUT /todos/1 with an empty title"
curl -s -i -X PUT $B/todos/1 -H "$J" -d '{"title":""}'
note 22 "PUT /todos/1 with malformed JSON"
curl -s -i -X PUT $B/todos/1 -H "$J" -d '{"title":"Broken"'
note 23 "POST /todos/1 with a title"
curl -s -i -X POST $B/todos/1 -H "$J" -d '{"title":"POST Updated Todo"}'
note 24 "DELETE /todos/1, then GET /todos/1 (expect 404)"
curl -s -i -X DELETE $B/todos/1
curl -s -i $B/todos/1
note 25 "OPTIONS /todos/1"
curl -s -i -X OPTIONS $B/todos/1
note 26 "PATCH /todos/999999"
curl -s -i -X PATCH $B/todos/999999 -H "$J" -d '{"title":"Should Not Work"}'
note 27 "GET /todos/1 with Accept: application/xml"
curl -s -i $B/todos/1 -H "$A"
note 28 "PUT /todos/1 with a JSON body and Accept: application/xml"
curl -s -i -X PUT $B/todos/1 -H "$J" -H "$A" -d '{"title":"XML Response Update"}'

printf "\n\nRestart the server now (java -jar runTodoManagerRestAPI-1.5.5.jar) to reset the data, then press Enter. "
read -r _ < /dev/tty

note 29 "GET /todos/1/tasksof"
curl -s -i $B/todos/1/tasksof
note 30 "POST /todos (new todo), POST /todos/3/tasksof {'id':'1'}, then GET /todos/3/tasksof"
curl -s -i -X POST $B/todos -H "$J" -d '{"title":"Task relationship test"}'
curl -s -i -X POST $B/todos/3/tasksof -H "$J" -d '{"id":"1"}'
curl -s -i $B/todos/3/tasksof
note 31 "POST /todos/3/tasksof {} (no id), then GET /todos/3/tasksof"
curl -s -i -X POST $B/todos/3/tasksof -H "$J" -d '{}'
curl -s -i $B/todos/3/tasksof
note 32 "GET /todos/999999/tasksof (non-existent todo)"
curl -s -i $B/todos/999999/tasksof
note 33 "DELETE /todos/1/tasksof/1"
curl -s -i -X DELETE $B/todos/1/tasksof/1
note 34 "GET, POST, PUT and PATCH on /todos/:id/tasksof/:id"
curl -s -i $B/todos/2/tasksof/1
curl -s -i -X POST $B/todos/1/tasksof/1 -H "$J" -d '{"id":"1"}'
curl -s -i -X PUT $B/todos/1/tasksof/1
curl -s -i -X PATCH $B/todos/1/tasksof/1
note 35 "DELETE /todos/2/tasksof/1 on a relationship that was already deleted"
curl -s -i -X DELETE $B/todos/2/tasksof/1
note 36 "DELETE /todos/1;ls/tasksof/1 (command injection attempt)"
curl -s -i -X DELETE "$B/todos/1%3Bls/tasksof/1"
note 37 "GET /todos/1/categories"
curl -s -i $B/todos/1/categories
note 38 "GET /todos/2/categories (todo with no categories)"
curl -s -i $B/todos/2/categories
note 39 "GET /todos/999999/categories (non-existent todo)"
curl -s -i $B/todos/999999/categories
note 40 "GET /todos/2/categories, POST {'id':'1'}, GET again"
curl -s -i $B/todos/2/categories
curl -s -i -X POST $B/todos/2/categories -H "$J" -d '{"id":"1"}'
curl -s -i $B/todos/2/categories
note 41 "Repeat POST /todos/2/categories {'id':'1'} (expect no duplicate)"
curl -s -i -X POST $B/todos/2/categories -H "$J" -d '{"id":"1"}'
curl -s $B/todos/2/categories
note 42 "PUT /todos/2/categories (405), then X-HTTP-Method-Override: POST"
curl -s -i -X PUT $B/todos/2/categories
curl -s -i -X DELETE $B/todos/1/categories -H "X-HTTP-Method-Override: POST" -H "$J" -d '{"id":"1"}'
