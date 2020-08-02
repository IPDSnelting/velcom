/// <reference types="cypress" />

function countRuns(alias: string) {
  return cy.get(alias).find('[data-cy="run_overview"]')
}

function loadMoreRuns(countAfter: number, alias: string) {
  const significantOnly = alias.indexOf('significant') > 0

  cy.route(
    `api/recently-benchmarked-commits?amount=${countAfter}&significant_only=${significantOnly}`
  ).as('fetch_more')

  cy.get(alias)
    .contains('load more')
    .click()

  cy.wait(['@fetch_more'])
}

context('Homepage', () => {
  beforeEach(() => {
    cy.server()

    cy.viewport(1000, 660)
    cy.visit('home')
  })

  it('displays recently benchmarked commits', () => {
    cy.route({
      method: 'GET',
      url: 'api/recently-benchmarked-commits?amount=10&significant_only=true',
      response: 'fixture:homepage_run_example.json'
    }).as('significantDefaultRoute')
    cy.visit('home')

    cy.contains('Recent Significant Commits').should('exist')

    cy.get('[data-cy="recent_significant"]')
      .find('[data-cy="run_overview"]')
      .as('run_overview')

    cy.get('@run_overview').contains('VelCom')

    cy.get('@run_overview').contains(
      '[frontend] Only show ECharts config menu on right click'
    )

    cy.get('@run_overview').contains(
      'I-Al-Istannen <i-al-istannen@users.noreply.github.com>'
    )

    cy.get('@run_overview').contains('frontend')
    cy.get('@run_overview').contains('build_time')

    cy.get('@run_overview').contains('051795dfafa7f78167cbb171ef643e22df746886')
  })

  it('fetches more significant runs', () => {
    cy.get('[data-cy="recent_significant"]').as('recent_significant')

    countRuns('@recent_significant').should('have.length', 10)

    loadMoreRuns(15, '@recent_significant')
    countRuns('@recent_significant').should('have.length', 15)

    // The pagination should be hidden for < 20 runs
    cy.get('@recent_significant')
      .contains('Items per page')
      .should('not.exist')

    loadMoreRuns(20, '@recent_significant')

    countRuns('@recent_significant').should('have.length', 20)

    // The pagination should be shown now
    cy.get('@recent_significant')
      .contains('Items per page')
      .should('exist')
  })

  it('fetches more normal runs', () => {
    cy.get('[data-cy="recent_runs"]').as('recent_runs')

    countRuns('@recent_runs').should('have.length', 5)

    loadMoreRuns(15, '@recent_runs')
    countRuns('@recent_runs').should('have.length', 15)

    // The pagination should be hidden for < 20 runs
    cy.get('@recent_runs')
      .contains('Items per page')
      .should('not.exist')

    // 25 loaded
    loadMoreRuns(25, '@recent_runs')

    // Only 20 displayed, it is paginated
    countRuns('@recent_runs').should('have.length', 20)

    // The pagination should be shown now
    cy.get('@recent_runs')
      .contains('Items per page')
      .should('exist')
  })

  it('should take you to the detail page when clicking the message', () => {
    cy.contains('Recent Significant Commits').should('exist')

    cy.get('[data-cy="recent_significant"]')
      .find('[data-cy="run_overview"]')
      .as('run_overview')

    cy.get('@run_overview')
      .contains('[frontend] Only show ECharts config men')
      .click()

    cy.location('pathname').should('include', 'commit-detail')
    cy.location('pathname').should(
      'include',
      '44bb5c8d-b20d-4bef-bdad-c92767dfa489'
    )
    cy.location('pathname').should(
      'include',
      '051795dfafa7f78167cbb171ef643e22df746886'
    )

    cy.go('back')
    cy.location('pathname').should('include', 'home')
  })

  it('should take you to the repo detail page when clicking the repo', () => {
    cy.contains('Recent Significant Commits').should('exist')

    cy.get('[data-cy="recent_significant"]')
      .find('[data-cy="run_overview"]')
      .as('run_overview')

    cy.get('@run_overview')
      .contains('VelCom')
      .click()

    cy.location('pathname').should('include', 'repo-detail')
    cy.location('pathname').should(
      'include',
      '44bb5c8d-b20d-4bef-bdad-c92767dfa489'
    )
    cy.go('back')
  })
})
