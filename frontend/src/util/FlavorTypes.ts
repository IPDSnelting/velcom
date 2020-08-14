// https://spin.atomicobject.com/2018/01/15/typescript-flexible-nominal-typing/#comment-604580
interface Flavoring<FlavorT> {
  _type?: FlavorT
}
export type Flavor<T, FlavorT> = T & Flavoring<FlavorT>
