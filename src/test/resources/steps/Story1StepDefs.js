const { Given, When, Then } = require('@cucumber/cucumber');
const api = require('../support/api.js');

const expect = api.expect;

Given('at least one TODO with a title containing {string} exists', async function (substring) {
    const todos = await api.list('/todos', 'todos');
    expect(todos.filter(t => t.title.includes(substring)).length).to.be.greaterThan(0);
});

Given('no TODO with the title {string} exists', async function (title) {
    const todos = await api.list('/todos', 'todos');
    expect(todos.filter(t => t.title === title).length).to.equal(0);
});

When('a user filters the list of todos by title name {string}', async function (title) {
    const res = await api.http().get('/todos').query({ title: title });
    this.response = res;
    this.filteredTodos = res.body.todos || [];
    if (this.filteredTodos.length === 0) {
        this.notification = `No todos found with title ${title}`;
    }
});

When('a user filters the list of todos by the title substring {string}', async function (substring) {
    const res = await api.http().get('/todos');
    this.response = res;
    this.filteredTodos = (res.body.todos || []).filter(t => t.title.includes(substring));
    if (this.filteredTodos.length === 0) {
        this.notification = `No todos found with title ${substring}`;
    }
});

Then('the filtered list contains {int} todo\\(s)', function (count) {
    expect(this.filteredTodos.length).to.equal(count);
});

Then('every todo in the filtered list has the title {string}', function (title) {
    this.filteredTodos.forEach(todo => expect(todo.title).to.equal(title));
});

Then('every todo in the filtered list has a title that contains {string}', function (substring) {
    this.filteredTodos.forEach(todo => expect(todo.title).to.include(substring));
});

Then('the user is notified of the completion of the filtering operation', function () {
    expect(this.response).to.have.status(200);
});

Then('the filtered list of todos is empty', function () {
    expect(this.filteredTodos.length).to.equal(0);
});
