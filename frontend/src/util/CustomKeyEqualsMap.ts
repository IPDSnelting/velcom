/**
 * A Map that uses a custom comparator that must be symmetric and reflexive for
 * keys. All lookup operations are in O(n), if the custom comparator  needs to
 * be used.
 */
export class CustomKeyEqualsMap<K, V> extends Map<K, V> {
  private readonly comparator: (first: K, second: K) => boolean

  constructor(
    iterable: Iterable<readonly [K, V]>,
    comparator: (first: K, second: K) => boolean
  ) {
    super(iterable)
    this.comparator = comparator
  }

  delete(key: K): boolean {
    let deleted: boolean = false

    for (const realKey of this.keys()) {
      if (this.comparator(realKey, key)) {
        deleted = super.delete(realKey) || deleted
      }
    }

    return deleted
  }

  get(key: K): V | undefined {
    if (super.get(key)) {
      return super.get(key)
    }
    const entry = Array.from(this.entries()).find(([realKey]) => {
      return this.comparator(realKey, key)
    })
    return entry ? entry[1] : undefined
  }

  has(key: K): boolean {
    if (super.has(key)) {
      return true
    }
    return (
      Array.from(this.keys()).find(realKey => this.comparator(key, realKey)) !==
      undefined
    )
  }
}
