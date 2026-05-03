import { NgModule, Optional, SkipSelf } from '@angular/core';

@NgModule({
  imports: [],
  exports: []
})
export class CoreModule {
  constructor(@Optional() @SkipSelf() parentModule: CoreModule) {
    if (parentModule) {
      throw new Error('CoreModule ya está cargado. Importar solo en AppModule.');
    }
  }
}