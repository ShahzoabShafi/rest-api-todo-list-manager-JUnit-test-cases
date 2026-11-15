const { Given, Then, Before, After } = require('@cucumber/cucumber');
const api = require('../support/api.js');

const expect = api.expect;

// Remember which objects exist before the scenario (the server's default data)...
Before(async function () {
    this.initialState = await api.snapshotIds();
});

// ...and afterwards delete everything the scenario created, restoring the initial state
After(async function () {
    if (this.initialState) {
        await api.restore(this.initialState);
    }
});

// --- Background steps shared by all stories ---

Given('the server is running', async function () {
    const res = await api.http().get('/');
    expect(res).to.have.status(200);
});

Given('the following TODOs exist:', async function (dataTable) {
    for (const todo of dataTable.hashes()) {
        await api.create('/todos', {
            title: todo.title,
            doneStatus: todo.doneStatus === 'true',
            description: todo.description
        });
    }
});

Given('the following categories exist:', async function (dataTable) {
    for (const category of dataTable.hashes()) {
        await api.create('/categories', { title: category.title, description: category.description });
    }
});

Given('the following projects exist:', async function (dataTable) {
    for (const project of dataTable.hashes()) {
        await api.create('/projects', {
            title: project.title,
            completed: project.completed === 'true',
            active: project.active === 'true',
            description: project.description
        });
    }
});

Given('the TODO titled {string} is associated with the category {string}', async function (todoTitle, categoryTitle) {
    await api.associateCategory(todoTitle, categoryTitle);
});

Given('the TODO titled {string} is a task of the project {string}', async function (todoTitle, projectTitle) {
    await api.linkTask(projectTitle, todoTitle);
});

// --- Given steps shared by several stories ---

Given('a TODO with the title {string} exists', async function (title) {
    await api.findByTitle('/todos', 'todos', title);
});

Given('no TODO with the id {string} exists', async function (id) {
    const todos = await api.list('/todos', 'todos');
    expect(todos.find(t => t.id === id)).to.equal(undefined);
    this.todoCountBefore = todos.length;
});

// --- Shared error check ---

Then('the user is notified of the non-existence error with the message {string}', function (message) {
    const expected = message.replace('<old id>', this.notedId).replace('<deleted id>', this.notedId);
    expect(this.notification, 'error message shown to the user').to.be.a('string');
    expect(this.notification).to.include(expected);
});