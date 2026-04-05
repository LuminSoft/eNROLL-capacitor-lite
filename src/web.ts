import { WebPlugin } from '@capacitor/core';

import type { EnrollPlugin } from './definitions';

export class EnrollWeb extends WebPlugin implements EnrollPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
