/// <reference types="cypress" />

function login(type?: 'Repo-Admin') {
  cy.get('#nav-bar').then($navBar => {
    if ($navBar.text().indexOf('Login') >= 0) {
      cy.get('#nav-bar').contains('Login').click()

      if (type) {
        cy.get('#login-dialog').contains('Repository-Admin').click()
        cy.get('#login-dialog')
          .get('[data-cy=repo-input]')
          .type('VelCom{enter}')
        cy.get('#login-dialog')
          .get('[data-cy=password-input]')
          .type('123456{enter}')
      } else {
        cy.get('#login-dialog').contains('Web-Admin').click()
        cy.get('#login-dialog')
          .get('[data-cy=password-input]')
          .type('12345{enter}')
      }
    }
  })
}

function logout() {
  cy.get('#nav-bar').then($navBar => {
    if ($navBar.text().indexOf('Logout') >= 0) {
      cy.get('#nav-bar').contains('Logout').click()
    }
  })
}

function addCommitToQueue(repoId: string, hash: string) {
  cy.visit(`run-detail/${repoId}/${hash}`)

  login()

  cy.route({
    method: 'POST',
    url: /queue\/commit\/.+/
  }).as('postToQueue')

  cy.get('[data-cy=initiate-benchmark]').click()

  cy.wait('@postToQueue')
}

function clearQueue() {
  cy.visit('/queue')
  login()
  cy.contains('Cancel all tasks').click()
  logout()
}

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

  it('displays commit in queue', () => {
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

  it.only('leads to task detail', () => {
    addCommitToQueue(
      '44bb5c8d-b20d-4bef-bdad-c92767dfa489',
      '9c952dc6173589f48874f95c64f014adcb7fd993'
    )
    cy.visit('/queue')

    cy.contains('Merge pull request #78 from').click()

    cy.waitFor('[@task-detail]')

    cy.contains('Show snackbar message')
    cy.contains(
      'I-Al-Istannen <I-Al-Istannen@users.noreply.github.com> authored at 2020-09-22 11:38'
    )
    cy.contains('GitHub <noreply@github.com> committed at 2020-09-22 11:38')
    cy.contains(
      'Merge pull request #78 from IPDSnelting/provide-manual-benchmark-feedback'
    )
      .should('exist')
      .and('have.attr', 'href')
      .and('include', 'run-detail')
      .and('include', '9c952dc6173589f48874f95c64f014adcb7fd993')
      .and('include', '44bb5c8d-b20d-4bef-bdad-c92767dfa489')
    cy.contains('9c952dc6173589f48874f95c64f014adcb7fd993').should('exist')

    cy.contains('Runner output').should('exist')
    cy.contains('No output received').should('exist')
  })

  it('shows admin buttons when logged in', () => {
    addCommitToQueue(
      '44bb5c8d-b20d-4bef-bdad-c92767dfa489',
      'a4159999515f26e8a53ee0cee19be8a925f3ca6c'
    )
    cy.visit('/queue')

    logout()

    cy.contains('Upload Tar').should('not.exist')
    cy.contains('Cancel all tasks').should('not.exist')
    cy.contains('Refetch all repos').should('not.exist')
    cy.get('.rocket').should('not.exist')
    cy.get('[data-cy=delete-queue-item]').should('not.exist')

    login()

    cy.contains('Upload Tar').should('exist')
    cy.contains('Cancel all tasks').should('exist')
    cy.contains('Refetch all repos').should('exist')
    cy.get('.rocket').should('exist')
    cy.get('[data-cy=delete-queue-item]').should('exist')
  })

  it('shows some admin buttons when logged in as repo admin', () => {
    addCommitToQueue(
      '44bb5c8d-b20d-4bef-bdad-c92767dfa489',
      'a4159999515f26e8a53ee0cee19be8a925f3ca6c'
    )
    cy.visit('/queue')

    logout()

    cy.contains('Upload Tar').should('not.exist')
    cy.contains('Cancel all tasks').should('not.exist')
    cy.contains('Refetch all repos').should('not.exist')
    cy.get('.rocket').should('not.exist')
    cy.get('[data-cy=delete-queue-item]').should('not.exist')

    login('Repo-Admin')

    cy.contains('Upload Tar').should('not.exist')
    cy.contains('Cancel all tasks').should('not.exist')
    cy.contains('Refetch all repos').should('not.exist')
    cy.get('.rocket').should('not.exist')
    cy.get('[data-cy=delete-queue-item]').should('not.exist')

    cy.contains('Cancel all tasks for <VelCom>').should('exist')
  })

  it('delete works', () => {
    clearQueue()
    addCommitToQueue(
      '44bb5c8d-b20d-4bef-bdad-c92767dfa489',
      'a4159999515f26e8a53ee0cee19be8a925f3ca6c'
    )

    cy.visit('/queue')

    cy.contains('[frontend] Fix standard deviation percent computation').should(
      'exist'
    )

    cy.get('[data-cy=delete-queue-item]').click()

    cy.contains('[frontend] Fix standard deviation percent computation').should(
      'not.exist'
    )
  })

  it('delete all works', () => {
    clearQueue()
    const commits = [
      {
        repoId: '44bb5c8d-b20d-4bef-bdad-c92767dfa489',
        hash: 'a4159999515f26e8a53ee0cee19be8a925f3ca6c',
        message: '[frontend] Fix standard deviation percent computation'
      },
      {
        repoId: '44bb5c8d-b20d-4bef-bdad-c92767dfa489',
        hash: '514442b558c9d08b260de9b1f1ae9744e1afd200',
        message: '[frontend] Fix errors overflowing tooltip activator button'
      },
      {
        repoId: 'd471b648-ce65-41e2-9c44-84fb82b73100',
        hash: '2ecf009141772328dd3b5aa33326873b145a778d',
        message: 'Little <img src=x onerror=alert(2)> tables we call him'
      }
    ]

    commits.forEach(({ repoId, hash }) => {
      addCommitToQueue(repoId, hash)
    })
    cy.visit('/queue')

    commits.forEach(({ message }) => {
      cy.contains(message).should('exist')
    })
    cy.contains('Cancel all tasks').click()

    commits.forEach(({ message }) => {
      cy.contains(message).should('not.exist')
    })
  })

  it('delete all as repo admin works', () => {
    clearQueue()
    const commits = [
      {
        repoId: '44bb5c8d-b20d-4bef-bdad-c92767dfa489',
        hash: 'a4159999515f26e8a53ee0cee19be8a925f3ca6c',
        message: '[frontend] Fix standard deviation percent computation',
        deleted: true
      },
      {
        repoId: '44bb5c8d-b20d-4bef-bdad-c92767dfa489',
        hash: '514442b558c9d08b260de9b1f1ae9744e1afd200',
        message: '[frontend] Fix errors overflowing tooltip activator button',
        deleted: true
      },
      {
        repoId: 'd471b648-ce65-41e2-9c44-84fb82b73100',
        hash: '2ecf009141772328dd3b5aa33326873b145a778d',
        message: 'Little <img src=x onerror=alert(2)> tables we call him',
        deleted: false
      }
    ]

    commits.forEach(({ repoId, hash }) => {
      addCommitToQueue(repoId, hash)
    })
    cy.visit('/queue')

    logout()
    login('Repo-Admin')

    commits.forEach(({ message }) => {
      cy.contains(message).should('exist')
    })
    cy.contains('Cancel all tasks for <VelCom>').click()

    commits.forEach(({ message, deleted }) => {
      cy.contains(message).should(deleted ? 'not.exist' : 'exist')
    })
  })
})
