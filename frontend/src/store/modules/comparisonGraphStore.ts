import { createModule } from 'vuex-class-component'

const VxModule = createModule({
  namespaced: 'comparisonGraphModule',
  strict: false
})

export class ComparisonGraphStore extends VxModule {}
