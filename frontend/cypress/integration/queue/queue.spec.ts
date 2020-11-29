/// <reference types="cypress" />

context('Queue', () => {
  beforeEach(() => {
    cy.server()

    cy.viewport(1000, 660)
  })

  it('requests correct endpoints', () => {
    cy.route('/queue').as('queue')

    cy.visit('queue')

    cy.waitFor('[@queue]')
  })

  it('displays queue empty message', () => {
    cy.route({
      method: 'GET',
      url: '/queue',
      response: '{"tasks":[],"runners":[]}'
    }).as('queue')

    cy.visit('queue')

    cy.waitFor('[@queue]')

    cy.contains('No commits are currently enqueued.').should('exist')
  })

  it('displays runner', () => {
    cy.route({
      method: 'GET',
      url: '/queue',
      response:
        '{"tasks":[],"runners":[{"name":"I-Al-VPS - Runner","info":"System: Linux amd64 4.9.0-13-amd64\\n' +
        'CPU:    Intel Xeon Processor (Skylake, IBRS) (2 threads)\\nMemory: 7797 MiB total, 4233 MiB available\\n"}]}'
    }).as('queue')

    cy.visit('queue')

    cy.waitFor('[@queue]')

    cy.contains('No commits are currently enqueued.').should('exist')
    cy.contains('I-Al-VPS - Runner').should('exist')
    cy.contains('System: Linux amd64').should('exist')
    cy.contains('CPU:').should('exist')
    cy.contains('7797 MiB total').should('exist')
  })

  it('refreshes queue every 10 seconds', () => {
    cy.route('/queue').as('queue')

    cy.visit('queue')

    cy.waitFor('[@queue]')

    cy.wait(8_000)

    cy.waitFor('[@queue]')
  })

  it.only('displays commit in queue', () => {
    cy.route({
      method: 'GET',
      url: '/queue',
      response: 'fixture:queue-with-one-commit.json'
    }).as('queue')

    cy.visit('queue')

    cy.waitFor('[@queue]')

    cy.contains(
      'Merge pull request #78 from IPDSnelting/provide-manual-benchmark-feedback'
    )
      .should('exist')
      .and('have.attr', 'href')
      .and('include', 'task-detail')
      .and('include', '67d41dca-67bf-4a21-b076-01e447033f47')
    cy.contains('9c952dc6173589f48874f95c64f014adcb7fd993').should('exist')

    cy.contains(
      'Merge pull request #78 from IPDSnelting/provide-manual-benchmark-feedback'
    )
      .parents('.v-card')
      .first()
      .find('.v-progress-linear')
      .should('exist')
  })

  it('displays tar in queue', () => {
    cy.route({
      method: 'GET',
      url: '/queue',
      response: 'fixture:queue-with-tar-files.json'
    }).as('queue')

    cy.visit('queue')

    cy.waitFor('[@queue]')

    cy.contains('My non-attached tar').should('exist')
    cy.contains('My Test tar').should('exist')
    cy.contains('6050f4e1-9f3f-405f-90fe-459e074f63f8').should('exist')
    cy.contains('ee78bea4-a142-4a7c-9e72-2b209b01b777').should('exist')

    cy.contains('My Test tar')
      .should('have.attr', 'href')
      .and('include', 'task-detail')
      .and('include', 'ee78bea4-a142-4a7c-9e72-2b209b01b777')

    cy.contains('My Test tar')
      .parents('.v-card')
      .first()
      .find('.repo-name')
      .should('exist')
      .should('have.text', 'VelCom')
  })

  it('refreshes running time every second', () => {
    cy.route({
      method: 'GET',
      url: '/queue',
      response: 'fixture:queue-with-one-commit.json'
    }).as('queue')

    cy.visit('queue')

    cy.waitFor('[@queue]')

    let lastTime: string = ''

    cy.contains(/Running on .+ for .+ seconds/).should(it => {
      // eslint-disable-next-line
      expect(it).to.exist

      lastTime = (it as any).first().text()
    })

    cy.contains('Running on', { timeout: 2000 }).should(it =>
      expect(it).to.not.contain(lastTime)
    )
  })
})
