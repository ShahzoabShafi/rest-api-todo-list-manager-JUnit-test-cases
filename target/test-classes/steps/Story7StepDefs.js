const { Given, When, Then } = require('@cucumber/cucumber');
const api = require('../support/api.js');

const expect = api.expect;

// The three filter steps differ only in the query string, so they share this helper
async function filterProjects(world, query) {
    const res = await api.http().get('/projects').query(query);
    world.response = res;                          // Yessine's "completion" step reads this.response
    world.filteredProjects = res.body.projects || [];
}

function filteredTitles(world) {
    return world.filteredProjects.map(p => p.title);
}

// --- Given steps ---

Given('no project with the title {string} exists', async function (title) {
    expect(await api.tryFindByTitle('/projects', 'projects', title)).to.equal(undefined);
});

// --- When steps ---

When('a user filters the list of projects by active {string}', async function (active) {
    await filterProjects(this, { active: active });
});

When('a user filters the list of projects by active {string} and completed {string}',
    async function (active, completed) {
        await filterProjects(this, { active: active, completed: completed });
    });

When('a user filters the list of projects by title {string}', async function (title) {
    await filterProjects(this, { title: title });
});

// --- Then steps ---

// The API returns booleans as strings ("true"/"false"), so compare as strings
Then('every project in the filtered list has active {string}', function (active) {
    for (const project of this.filteredProjects) {
        expect(String(project.active), `active of "${project.title}"`).to.equal(active);
    }
});

Then('every project in the filtered list has active {string} and completed {string}',
    function (active, completed) {
        for (const project of this.filteredProjects) {
            expect(String(project.active), `active of "${project.title}"`).to.equal(active);
            expect(String(project.completed), `completed of "${project.title}"`).to.equal(completed);
        }
    });

Then('the filtered list of projects includes {string}', function (title) {
    expect(filteredTitles(this)).to.include(title);
});

Then('the filtered list of projects does not include {string}', function (title) {
    expect(filteredTitles(this)).to.not.include(title);
});

Then('the filtered list of projects is empty', function () {
    expect(this.filteredProjects).to.have.lengthOf(0);
});

// An unmatched filter is not an error: the API answers with an empty list instead of a 404
Then('no error is returned for an unmatched filter', function () {
    expect(this.response.status).to.be.below(400);
    expect(this.response.body).to.not.have.property('errorMessages');
});
