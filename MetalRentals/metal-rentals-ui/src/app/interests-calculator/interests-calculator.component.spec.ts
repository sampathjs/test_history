import {async, ComponentFixture, TestBed} from '@angular/core/testing';

import {InterestsCalculatorComponent} from './interests-calculator.component';

describe('InterestCalculatorComponent', () => {
    let component: InterestsCalculatorComponent;
    let fixture: ComponentFixture<InterestsCalculatorComponent>;

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [InterestsCalculatorComponent]
        })
            .compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(InterestsCalculatorComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
