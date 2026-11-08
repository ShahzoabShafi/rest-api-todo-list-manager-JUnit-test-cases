const { Given, When, Then } = require('@cucumber/cucumber');
const api = require('../support/api.js');

const expect = api.expect;

function parseValue(value) {
    if (value === 'true') return true;
    if (value === 'false') return false;
    return value; // invalid values are sent as-is so the API can reject them
}

Given('a TODO with the title {string} and doneStatus {string} exists', async function (title, doneStatus) {
    const todo = await api.findByTitle('/todos', 'todos', title);
    expect(String(todo.doneStatus)).to.equal(doneStatus);
});

When('a user updates the doneStatus of the TODO titled {string} to {string}', async function (title, value) {
    const todo = await api.findByTitle('/todos', 'todos', title);
    this.original = todo;
    const res = await api.http().post(`/todos/${todo.id}`).send({ doneStatus: parseValue(value) });
    api.record(this, res);
});

When('a user updates the doneStatus of the TODO with id {string} to {string}', async function (id, value) {
    const res = await api.http().post(`/todos/${id}`).send({ doneStatus: parseValue(value) });
    api.record(this, res);
});

Then('the TODO titled {string} has doneStatus {string}', async function (title, doneStatus) {
    const todo = await api.findByTitle('/todos', 'todos', title);
    expect(String(todo.doneStatus)).to.equal(doneStatus);
});

Then('the title and description of the TODO titled {string} are unchanged', async function (title) {
    const todo = await api.findByTitle('/todos', 'todos', title);
    expect(todo.title).to.equal(this.original.title);
    expect(todo.description).to.equal(this.original.description);
});

Then('the user is notified of the completion of the update operation', function () {
    expect(this.response).to.have.status(200);
});

Then('no TODO is created or modified', async function () {
    const todos = await api.list('/todos', 'todos');
    expect(todos.length).to.equal(this.todoCountBefore);
});

Then('the TODO titled {string} keeps its original doneStatus', async function (title) {
    const todo = await api.findByTitle('/todos', 'todos', title);
    expect(String(todo.doneStatus)).to.equal(String(this.original.doneStatus));
});

Then('the user is notified of the validation error with the message {string}', function (message) {
    expect(this.response.status).to.be.within(400, 499);
    expect(this.notification).to.include(message);
});
