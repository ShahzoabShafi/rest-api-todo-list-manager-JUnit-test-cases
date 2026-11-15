const { Given, When, Then } = require('@cucumber/cucumber');
const api = require('../support/api.js');

const expect = api.expect;

async function categoriesOf(todoTitle) {
    const todo = await api.findByTitle('/todos', 'todos', todoTitle);
    return api.list(`/todos/${todo.id}/categories`, 'categories');
}

Given('a category with the title {string} exists', async function (title) {
    await api.findByTitle('/categories', 'categories', title);
});

Given('the TODO titled {string} has no categories', async function (title) {
    expect((await categoriesOf(title)).length).to.equal(0);
});

Given('the TODO titled {string} is already associated with the category {string}', async function (todoTitle, categoryTitle) {
    await api.associateCategory(todoTitle, categoryTitle);
});

Given('no category with the id {string} exists', async function (id) {
    const categories = await api.list('/categories', 'categories');
    expect(categories.find(c => c.id === id)).to.equal(undefined);
});

When('a user associates the TODO titled {string} with the category {string}', async function (todoTitle, categoryTitle) {
    const todo = await api.findByTitle('/todos', 'todos', todoTitle);
    const category = await api.findByTitle('/categories', 'categories', categoryTitle);
    const res = await api.http().post(`/todos/${todo.id}/categories`).send({ id: category.id });
    api.record(this, res);
});

When('a user associates the TODO titled {string} with the category id {string}', async function (todoTitle, categoryId) {
    const todo = await api.findByTitle('/todos', 'todos', todoTitle);
    this.categoriesBefore = (await categoriesOf(todoTitle)).length;
    const res = await api.http().post(`/todos/${todo.id}/categories`).send({ id: categoryId });
    api.record(this, res);
});

When('a user associates the TODO with id {string} with the category {string}', async function (todoId, categoryTitle) {
    const category = await api.findByTitle('/categories', 'categories', categoryTitle);
    this.categoryTodosBefore = (await api.list(`/categories/${category.id}/todos`, 'todos')).length;
    const res = await api.http().post(`/todos/${todoId}/categories`).send({ id: category.id });
    api.record(this, res);
});

Then('the categories of the TODO titled {string} contain {string}', async function (todoTitle, categoryTitle) {
    const categories = await categoriesOf(todoTitle);
    expect(categories.map(c => c.title)).to.include(categoryTitle);
});

Then('the todos of the category {string} contain the TODO titled {string}', async function (categoryTitle, todoTitle) {
    const category = await api.findByTitle('/categories', 'categories', categoryTitle);
    const todos = await api.list(`/categories/${category.id}/todos`, 'todos');
    expect(todos.map(t => t.title)).to.include(todoTitle);
});

Then('the user is notified of the completion of the association operation', function () {
    expect(this.response).to.have.status(201);
});

Then('the TODO titled {string} has {int} categories', async function (title, count) {
    expect((await categoriesOf(title)).length).to.equal(count);
});

Then('the TODO titled {string} has no new categories', async function (title) {
    expect((await categoriesOf(title)).length).to.equal(this.categoriesBefore);
});

Then('the todos of the category {string} are unchanged', async function (categoryTitle) {
    const category = await api.findByTitle('/categories', 'categories', categoryTitle);
    const todos = await api.list(`/categories/${category.id}/todos`, 'todos');
    expect(todos.length).to.equal(this.categoryTodosBefore);
});
