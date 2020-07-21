import {TestBed} from '@angular/core/testing';

import {MetalRentalsService} from './metal-rentals.service';

describe('MetalRentalsService', () => {
    beforeEach(() => TestBed.configureTestingModule({}));

    it('should be created', () => {
        const service: MetalRentalsService = TestBed.get(MetalRentalsService);
        expect(service).toBeTruthy();
    });
});
