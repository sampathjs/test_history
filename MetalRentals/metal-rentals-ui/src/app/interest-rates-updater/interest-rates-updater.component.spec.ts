import {async, ComponentFixture, TestBed} from '@angular/core/testing';

import {InterestRatesUpdaterComponent} from './interest-rates-updater.component';

describe('InterestRatesUpdaterComponent', () => {
    let component: InterestRatesUpdaterComponent;
    let fixture: ComponentFixture<InterestRatesUpdaterComponent>;

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [InterestRatesUpdaterComponent]
        })
            .compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(InterestRatesUpdaterComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
