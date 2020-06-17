import {async, ComponentFixture, TestBed} from '@angular/core/testing';

import {DocumentsGeneratorComponent} from './documents-generator.component';

describe('DocumentGeneratorComponent', () => {
    let component: DocumentsGeneratorComponent;
    let fixture: ComponentFixture<DocumentsGeneratorComponent>;

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [DocumentsGeneratorComponent]
        })
            .compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(DocumentsGeneratorComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
