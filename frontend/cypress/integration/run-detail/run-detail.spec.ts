/// <reference types="cypress" />

function assertSorted(
  array: string[],
  mode: 'ascending' | 'descending',
  sortFunction: (a: string, b: string) => number
) {
  const sorted = array.slice()
  sorted.sort((a, b) =>
    mode === 'ascending' ? sortFunction(a, b) : -sortFunction(a, b)
  )
  assert.deepEqual(array, sorted, 'Expected array to be sorted')
}

function sortText(a: string, b: string) {
  return a.localeCompare(b)
}

function sortNumber(a: string, b: string) {
  const first = parseFloat(a.replace(',', '.'))
  const second = parseFloat(b.replace(',', '.'))

  if (a.trim() !== 'N/A') {
    assert.isNotNaN(first)
  }
  if (b.trim() !== 'N/A') {
    assert.isNotNaN(second)
  }

  return first - second
}

function sortPercent(a: string, b: string) {
  return sortNumber(a, b)
}

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

  it('sorts by value', () => {
    cy.get('.measurement-table').as('measurement-table')

    const sortableColumns = [
      { name: 'Benchmark', index: 1, sortFunction: sortText },
      { name: 'Metric', index: 2, sortFunction: sortText },
      { name: 'Unit', index: 3, sortFunction: sortText },
      { name: 'Value', index: 4, sortFunction: sortNumber },
      { name: 'Stddev', index: 5, sortFunction: sortNumber },
      { name: 'Stddev %', index: 6, sortFunction: sortPercent },
      { name: 'Change', index: 7, sortFunction: sortNumber },
      { name: 'Change %', index: 8, sortFunction: sortPercent }
    ]

    sortableColumns.forEach(({ name, index, sortFunction }) => {
      cy.get(
        `[aria-label="${name}: Not sorted. Activate to sort ascending."]`
      ).as('sortButton')

      const isSorted = (mode: 'ascending' | 'descending') => {
        cy.get('@measurement-table').within(() => {
          cy.get('tr').within(() => {
            cy.get(`td:nth-child(${index})`).should(a => {
              const values = a.toArray().map(it => it.textContent!)

              assertSorted(values, mode, sortFunction)
            })
          })
        })
      }

      cy.get('@sortButton').click()
      isSorted('ascending')

      cy.get('@sortButton').click()
      isSorted('descending')

      cy.get('@sortButton').click()
    })
  })
})
