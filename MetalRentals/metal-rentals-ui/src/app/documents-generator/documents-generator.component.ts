import {Component, Input} from '@angular/core';
import {Results} from "../services/results";
import {Backend} from "../services/backend";
import {finalize} from "rxjs/operators";

@Component({
    selector   : 'app-document-generator',
    templateUrl: './documents-generator.component.html',
    styleUrls  : ['./documents-generator.component.css', '../component-common.css']
})
export class DocumentsGeneratorComponent {

    @Input() region: string;
    @Input() interests: Results;

    readonly TOOLTIP_HINT = "The process can take around half hour totally. Feel free to close the browser after starting the process";
    readonly TOOLTIP_WARN = "Can't generate documents yet because the interests are not calculated";
    readonly TOOLTIP_CLICKED = "The process has been started. Summary emails will be sent once the process is finished";

    generateStatements: boolean = true;
    bookCashDeals: boolean = true;
    isGenerateClicked: boolean = false;
    isBusy: boolean = false;
    error: string;

    constructor(private backend: Backend) {
    }

    get readyToGenerate(): boolean {
        return !this.isGenerateClicked && this.interests && (this.generateStatements || this.bookCashDeals);
    }

    get tooltip(): string {
        return this.interests ? (this.isGenerateClicked ? this.TOOLTIP_CLICKED : this.TOOLTIP_HINT) : this.TOOLTIP_WARN;
    }

    generateClicked() {
        this.isGenerateClicked = true;
        this.isBusy = true;
        this.backend.generateDocuments(this.region, this.interests, this.generateStatements, this.bookCashDeals)
            .pipe(finalize(() => this.isBusy = false))
            .subscribe(() => {
                this.error = undefined;
            }, error => {
                this.error = error;
            });
    }
}
