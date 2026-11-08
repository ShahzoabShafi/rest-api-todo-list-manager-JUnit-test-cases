const { Given, When, Then } = require('@cucumber/cucumber');
const api = require('../support/api.js');

const expect = api.expect;

Given('the TODO titled {string} is not a task of any project', async function (title) {
    const todo = await api.findByTitle('/todos', 'todos', title);
    expect((await api.list(`/todos/${todo.id}/tasksof`, 'projects')).length).to.equal(0);
});

When('a user requests the projects related to the TODO titled {string}', async function (title) {
    const todo = await api.findByTitle('/todos', 'todos', title);
    const res = await api.http().get(`/todos/${todo.id}/tasksof`);
    api.record(this, res);
    this.projects = res.body.projects || [];
});

When('a user requests the projects related to the TODO with id {string}', async function (id) {
    const res = await api.http().get(`/todos/${id}/tasksof`);
    api.record(this, res);
    this.projects = res.body.projects || [];
});

Then('the list of projects contains {int} project\\(s)', function (count) {
    expect(this.projects.length).to.equal(count);
});

Then('the list of projects contains the project {string}', function (title) {
    expect(this.projects.map(p => p.title)).to.include(title);
});

Then('the list of projects is empty', function () {
    expect(this.projects.length).to.equal(0);
});

Then('no list of projects is returned', function () {
    expect(this.projects.length).to.equal(0);
});

Then('the user is notified of the completion of the retrieval operation', function () {
    expect(this.response).to.have.status(200);
});
