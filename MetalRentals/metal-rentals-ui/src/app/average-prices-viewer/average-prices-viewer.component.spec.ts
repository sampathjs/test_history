import {async, ComponentFixture, TestBed} from '@angular/core/testing';

import {AveragePricesViewerComponent} from './average-prices-viewer.component';

describe('AveragePricesViewerComponent', () => {
    let component: AveragePricesViewerComponent;
    let fixture: ComponentFixture<AveragePricesViewerComponent>;

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [AveragePricesViewerComponent]
        })
            .compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(AveragePricesViewerComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
