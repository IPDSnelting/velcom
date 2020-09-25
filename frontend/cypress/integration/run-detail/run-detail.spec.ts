/// <reference types="cypress" />

context('Run Detail', () => {
  beforeEach(() => {
    cy.server()

    cy.route({
      method: 'GET',
      url:
        '/api/commit/44bb5c8d-b20d-4bef-bdad-c92767dfa489/147413df6d64ab54ceafbd5b0a38a1d72cefed5d',
      response: 'fixture:run-detail-commit-info.json'
    }).as('commit-info')
    cy.route({
      method: 'GET',
      url:
        '/api/run/fd9e7bf9-e8e7-4866-b5a4-87f4735c942f?all_values=true&diff_prev=true',
      response: 'fixture:run-detail-run-info.json'
    }).as('run-info')

    cy.viewport(1000, 660)

    cy.visit(
      'run-detail/44bb5c8d-b20d-4bef-bdad-c92767dfa489/147413df6d64ab54ceafbd5b0a38a1d72cefed5d'
    )
  })

  it('requests correct endpoints', () => {
    cy.waitFor('[@commit-info, @run-info]')
  })

  it('contains commit message', () => {
    // summary
    cy.contains(
      'Merge pull request #89 from IPDSnelting/fix-units-sometimes-missing'
    ).should('exist')

    // detail message
    cy.contains(
      'Fix units and interpretations missing for a metric after semi-failed run'
    ).should('exist')
  })

  it('contains author', () => {
    cy.contains('Garmelon <joscha@plugh.de>').should('exist')
  })

  it('contains parents and children', () => {
    // Parents
    cy.contains('[runner] Fix bench output parser tests').should('exist')
    cy.contains(
      'Merge pull request #85 from IPDSnelting/minor-frontend-improvements'
    ).should('exist')

    // Child
    cy.contains(
      'Merge pull request #92 from IPDSnelting/significant-runs'
    ).should('exist')
  })

  it('contains commit hash and repo id', () => {
    cy.contains('147413df6d64ab54ceafbd5b0a38a1d72cefed5d').should('exist')

    cy.contains('147413df6d64ab54ceafbd5b0a38a1d72cefed5d')
      .parentsUntil('.v-card')
      .first()
      .contains('VelCom')
      .should(it => {
        expect(it)
          .to.have.attr('href')
          .contains('44bb5c8d-b20d-4bef-bdad-c92767dfa489')
      })
  })

  it('contains valid run information', () => {
    cy.get('[data-cy="run-information"]').as('run-info')

    cy.get('@run-info')
      .contains('I-Al-VPS - Runner')
      .should('exist')
    cy.get('@run-info')
      .contains('7797 MiB total, 4239 MiB available')
      .should('exist')
    cy.get('@run-info')
      .contains('Intel Xeon Processor (Skylake, IBRS) (2 threads)')
      .should('exist')

    cy.get('@run-info')
      .contains('1 minutes and 31 seconds')
      .should('exist')
    cy.get('@run-info')
      .contains('2020-09-24 23:19')
      .should('exist')
    cy.get('@run-info')
      .contains('2020-09-24 23:17')
      .should('exist')
    cy.get('@run-info')
      .contains('commit by Listener')
      .should('exist')
  })
})
