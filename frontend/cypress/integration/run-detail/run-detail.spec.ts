/// <reference types="cypress" />

context('Run Detail', () => {
  beforeEach(() => {
    cy.server()

    cy.viewport(1000, 660)

    cy.visit(
      'run-detail/44bb5c8d-b20d-4bef-bdad-c92767dfa489/deec6c90541a9064849af7011fa9d7c6f40a3de1'
    )
  })

  it('requests correct endpoints', () => {
    cy.waitFor('[@commit-info, @run-info]')
  })

  it('contains commit message', () => {
    // summary
    cy.contains(
      'Merge pull request #154 from IPDSnelting/navigate-and-refresh'
    ).should('exist')

    // detail message
    cy.contains('Navigate and refresh').should('exist')
  })

  it('contains author', () => {
    cy.contains('oudemia <52839122+oudemia@users.noreply.github.com>').should(
      'exist'
    )
  })

  it('contains parents and children', () => {
    // Parents
    cy.contains('[backend] Add some doc comments').should('exist')
    cy.contains(
      '[frontend] use router links rather than router.push for navigation'
    ).should('exist')

    // Child
    cy.contains('Merge pull request #156 from IPDSnelting/small-bugs-1').should(
      'exist'
    )
  })

  it('contains commit hash and repo id', () => {
    cy.contains('deec6c90541a9064849af7011fa9d7c6f40a3de1').should('exist')

    cy.contains('deec6c90541a9064849af7011fa9d7c6f40a3de1')
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

    cy.get('@run-info').contains('I-Al-VPS - Runner').should('exist')
    cy.get('@run-info')
      .contains('7797 MiB total, 4280 MiB available')
      .should('exist')
    cy.get('@run-info')
      .contains('Intel Xeon Processor (Skylake, IBRS) (2 threads)')
      .should('exist')

    cy.get('@run-info').contains('4 minutes and 12 seconds').should('exist')
    cy.get('@run-info').contains('2020-10-31 15:46').should('exist')
    cy.get('@run-info').contains('2020-10-31 15:51').should('exist')
    cy.get('@run-info').contains('commit by Listener').should('exist')
  })
})
