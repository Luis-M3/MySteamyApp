import { Component, Output, EventEmitter, OnDestroy } from '@angular/core';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';

@Component({
  selector: 'app-input',
  templateUrl: './input.component.html',
  styleUrls: ['./input.component.scss'],
  standalone: false,
})
export class InputComponent implements OnDestroy {
  @Output() searched = new EventEmitter<string>();

  private searchSubject = new Subject<string>();
  private destroy$ = new Subject<void>();

  constructor() {
    this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(query => this.searched.emit(query));
  }

  onInput(event: CustomEvent): void {
    this.searchSubject.next(event.detail.value ?? '');
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}