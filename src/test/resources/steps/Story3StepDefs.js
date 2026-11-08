const { Given, When, Then } = require('@cucumber/cucumber');
const api = require('../support/api.js');

const expect = api.expect;

Given('the user has found the TODO titled {string} in the list of todos and noted its id', async function (title) {
    const todos = await api.list('/todos', 'todos');
    const todo = todos.find(t => t.title === title);
    expect(todo, `"${title}" should be in the list`).to.not.equal(undefined);
    this.notedId = todo.id;
    this.notedTitle = title;
    this.todosBefore = todos;
    this.todoCountBefore = todos.length;
});

Given('that TODO has since been deleted', async function () {
    const res = await api.http().delete(`/todos/${this.notedId}`);
    expect(res).to.have.status(200);
    this.todoCountBefore = (await api.list('/todos', 'todos')).length;
});

// --- Steps for the "delete by id of the TODO titled X" wording ---

When('a user deletes the TODO using the id of the TODO titled {string}', async function (title) {
    const todos = await api.list('/todos', 'todos');
    const todo = todos.find(t => t.title === title);
    expect(todo, `"${title}" should exist`).to.not.equal(undefined);
    this.notedId = todo.id;
    this.todosBefore = todos;
    this.todoCountBefore = todos.length;
    const res = await api.http().delete(`/todos/${todo.id}`);
    api.record(this, res);
});

Given('the user has already deleted the TODO titled {string} using its id', async function (title) {
    const todo = await api.findByTitle('/todos', 'todos', title);
    this.notedId = todo.id;
    const res = await api.http().delete(`/todos/${todo.id}`);
    expect(res).to.have.status(200);
    this.todoCountBefore = (await api.list('/todos', 'todos')).length;
});

When('a user deletes the TODO using the same id again', async function () {
    const res = await api.http().delete(`/todos/${this.notedId}`);
    api.record(this, res);
});

When('a user deletes the TODO using that id', async function () {
    const res = await api.http().delete(`/todos/${this.notedId}`);
    api.record(this, res);
});

When('a user deletes the TODO using the id {string}', async function (id) {
    const res = await api.http().delete(`/todos/${id}`);
    api.record(this, res);
});

Then('the TODO titled {string} no longer exists', async function (title) {
    expect(await api.tryFindByTitle('/todos', 'todos', title)).to.equal(undefined);
});

Then('the total number of todos decreases by {int}', async function (n) {
    const todos = await api.list('/todos', 'todos');
    expect(todos.length).to.equal(this.todoCountBefore - n);
});

Then('the other todos are unchanged', async function () {
    const after = (await api.list('/todos', 'todos')).map(t => t.id).sort();
    const expected = this.todosBefore.filter(t => t.id !== this.notedId).map(t => t.id).sort();
    expect(after).to.deep.equal(expected);
});

Then('the user is notified of the completion of the delete operation', function () {
    expect(this.response).to.have.status(200);
});

Then('the category {string} still exists', async function (title) {
    await api.findByTitle('/categories', 'categories', title);
});

Then('the project {string} still exists', async function (title) {
    await api.findByTitle('/projects', 'projects', title);
});

Then('the category {string} no longer lists the TODO titled {string}', async function (categoryTitle, todoTitle) {
    const category = await api.findByTitle('/categories', 'categories', categoryTitle);
    const todos = await api.list(`/categories/${category.id}/todos`, 'todos');
    expect(todos.find(t => t.title === todoTitle)).to.equal(undefined);
});

Then('the project {string} no longer lists the TODO titled {string} as a task', async function (projectTitle, todoTitle) {
    const project = await api.findByTitle('/projects', 'projects', projectTitle);
    const tasks = await api.list(`/projects/${project.id}/tasks`, 'todos');
    expect(tasks.find(t => t.title === todoTitle)).to.equal(undefined);
});

Then('the total number of todos is unchanged', async function () {
    const todos = await api.list('/todos', 'todos');
    expect(todos.length).to.equal(this.todoCountBefore);
});
