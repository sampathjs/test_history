import {Component, Input} from '@angular/core';

@Component({
    selector   : 'app-error-indicator',
    templateUrl: './error-indicator.component.html',
    styleUrls  : ['./error-indicator.component.css']
})
export class ErrorIndicatorComponent {

    @Input() exception: any;

    formatError(): string {
        let msg: string = this.exception.status == "401" ? "Incorrect password" : "No message available";
        if (this.exception.error) {
            msg = this.exception.error.message;
        }
        return `${this.exception.status} - ${msg}`;
    }
}
