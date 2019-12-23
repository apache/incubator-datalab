import { TestBed } from '@angular/core/testing';

import { ProgressBarService } from './progress-bar.service';

describe('ProgressBarService', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('should be created', () => {
    const service: ProgressBarService = TestBed.get(ProgressBarService);
    expect(service).toBeTruthy();
  });
});
