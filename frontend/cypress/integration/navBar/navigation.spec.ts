/// <reference types="cypress" />

context('NavigationBar', () => {
  beforeEach(() => {
    cy.viewport(1000, 660)
    cy.visit('home')
  })

  it('displays burger menu only on screens <960 px wide', () => {
    cy.viewport(959, 660).then(() => {
      cy.get('#nav-bar').get('[data-cy=nav-burger]').should('exist')
    })
    cy.viewport(960, 660).then(() => {
      cy.get('#nav-bar')
        .get('.v-app-bar__nav-icon > .v-btn__content')
        .should('exist')
    })
  })

  it('allows navigation between the different views', () => {
    cy.location('pathname').should('include', 'home')

    cy.get('#nav-bar').contains('Repo Comparison').click()
    cy.location('pathname').should('include', 'repo-comparison')

    cy.go('back')
    cy.location('pathname').should('not.include', 'repo-comparison')

    cy.go('forward')
    cy.location('pathname').should('include', 'repo-comparison')

    cy.get('#nav-bar').contains('Repo Detail').click()
    cy.location('pathname').should('include', 'repo-detail')

    cy.get('#nav-bar').contains('Queue').click()
    cy.location('pathname').should('include', 'queue')

    cy.get('#nav-bar').contains('About').click()
    cy.location('pathname').should('include', 'about')
  })

  it('allows logging in and out as site admin', () => {
    cy.get('#nav-bar').should('contain', 'Login')

    cy.get('#nav-bar').contains('Login').click()

    cy.get('#login-dialog').should('be.visible')

    cy.get('#login-dialog').contains('Repository-Admin').click()
    cy.get('#login-dialog')
      .get('[data-cy=Repository-Admin]')
      .should('be.checked')

    cy.get('#login-dialog').contains('Web-Admin').click()
    cy.get('#login-dialog').get('[data-cy=Web-Admin]').should('be.checked')

    cy.get('#login-dialog').get('[data-cy=password-input]').type('12345{enter}')

    cy.get('#nav-bar').should('contain', 'Logout')

    cy.get('#nav-bar').contains('Logout').click()

    cy.get('#nav-bar').should('contain', 'Login')
  })

  // noinspection DuplicatedCode
  it('allows logging in and out as repo admin', () => {
    cy.get('#nav-bar').should('contain', 'Login')

    cy.get('#nav-bar').contains('Login').click()

    cy.get('#login-dialog').should('be.visible')

    cy.get('#login-dialog').contains('Repository-Admin').click()
    cy.get('#login-dialog')
      .get('[data-cy=Repository-Admin]')
      .should('be.checked')

    cy.get('#login-dialog').get('[data-cy=repo-input]').type('VelCom{enter}')
    cy.get('#login-dialog')
      .get('[data-cy=password-input]')
      .type('123456{enter}')

    cy.get('#nav-bar').should('contain', 'Logout')

    cy.get('#nav-bar').contains('Logout').click()

    cy.get('#nav-bar').should('contain', 'Login')
  })
})
