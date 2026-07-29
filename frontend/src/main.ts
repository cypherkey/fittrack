import { platformBrowser } from '@angular/platform-browser';
import { AppModule } from './app/app-module';

// Angular 21+ defaults to zoneless change detection (no zone.js).
platformBrowser()
  .bootstrapModule(AppModule)
  .catch((err) => console.error(err));
