import { DatePipe } from '@angular/common';
import { Component, ElementRef, inject, OnInit, signal, ViewChild } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Api, problem } from '../core/api';
import { Employee, EmployeeInput, ReferenceData } from '../core/models';
import { MoneyPipe } from '../core/money.pipe';

function positiveSalary(control: AbstractControl): ValidationErrors | null {
  const value: string = String(control.value ?? '');
  if (!value) return null;
  return /^(0|[1-9]\d{0,9})(\.\d{1,2})?$/.test(value) && BigInt(value.replace('.', '')) > 0n
    ? null
    : { salary: true };
}

@Component({
  selector: 'app-employee-form',
  imports: [ReactiveFormsModule, RouterLink, MoneyPipe, DatePipe],
  templateUrl: './employee-form.html',
  styleUrl: './employee-form.scss',
})
export class EmployeeForm implements OnInit {
  private readonly api = inject(Api);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  @ViewChild('errorSummary') private errorSummary?: ElementRef<HTMLElement>;
  readonly isNew = !this.route.snapshot.paramMap.get('id');
  readonly reference = signal<ReferenceData | null>(null);
  readonly employee = signal<Employee | null>(null);
  readonly busy = signal(false);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly conflict = signal(false);
  readonly success = signal('');
  readonly serverFields = signal<Record<string, string>>({});
  readonly query = this.route.snapshot.queryParams;
  readonly nameFields = [
    { key: 'firstName', label: 'First name', type: 'text', autocomplete: 'given-name', max: 80 },
    { key: 'lastName', label: 'Last name', type: 'text', autocomplete: 'family-name', max: 80 },
    { key: 'email', label: 'Work email', type: 'email', autocomplete: 'email', max: 254 },
  ] as const;
  readonly form = inject(FormBuilder).nonNullable.group({
    employeeCode: ['', [Validators.required, Validators.pattern('[A-Z0-9][A-Z0-9-]{1,29}')]],
    firstName: ['', [Validators.required, Validators.maxLength(80), Validators.pattern(/\S/)]],
    lastName: ['', [Validators.required, Validators.maxLength(80), Validators.pattern(/\S/)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    countryCode: ['US', Validators.required],
    departmentCode: ['ENGINEERING', Validators.required],
    annualSalary: ['', [Validators.required, positiveSalary]],
    currencyCode: ['USD', Validators.required],
  });
  async ngOnInit(): Promise<void> {
    try {
      this.reference.set(await firstValueFrom(this.api.referenceData()));
      if (!this.isNew) await this.load();
    } catch (error) {
      this.error.set(problem(error).detail ?? 'Employee could not be loaded.');
    } finally {
      this.loading.set(false);
    }
  }
  async load(): Promise<void> {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) return;
    this.loading.set(true);
    this.error.set('');
    this.conflict.set(false);
    this.serverFields.set({});
    try {
      const employee = await firstValueFrom(this.api.employee(id));
      this.employee.set(employee);
      this.form.patchValue(employee);
      this.form.markAsPristine();
    } catch (error) {
      this.error.set(problem(error).detail ?? 'Could not reload employee.');
    } finally {
      this.loading.set(false);
    }
  }
  fieldError(field: keyof EmployeeInput): string {
    const server = this.serverFields()[field];
    if (server) return server;
    if (field === 'version') return '';
    const control = this.form.controls[field];
    if (!control.touched || !control.invalid) return '';
    if (control.hasError('required')) return 'This field is required.';
    if (control.hasError('email')) return 'Enter a valid email address.';
    if (field === 'annualSalary')
      return 'Enter 0.01–9,999,999,999.99 with at most two decimal places.';
    if (field === 'employeeCode') return 'Use 2–30 uppercase letters, digits, or hyphens.';
    return 'Check this value and its length.';
  }
  private showError(error: unknown): void {
    const details = problem(error);
    this.error.set(details.detail ?? 'Could not save employee.');
    this.serverFields.set(details.fieldErrors ?? {});
    this.conflict.set(details.code === 'STALE_EMPLOYEE');
    setTimeout(() => this.errorSummary?.nativeElement.focus());
  }
  async save(): Promise<void> {
    if (this.busy()) return;
    this.form.markAllAsTouched();
    this.serverFields.set({});
    this.error.set('');
    this.success.set('');
    this.conflict.set(false);
    if (this.form.invalid) {
      this.error.set('Check the highlighted fields before saving.');
      setTimeout(() => this.errorSummary?.nativeElement.focus());
      return;
    }
    this.busy.set(true);
    try {
      const existing = this.employee();
      const input: EmployeeInput = {
        ...this.form.getRawValue(),
        ...(existing ? { version: existing.version } : {}),
      };
      const result = await firstValueFrom(
        existing ? this.api.update(existing.id, input) : this.api.create(input),
      );
      if (!existing) {
        await this.router.navigate(['/employees', result.id], { queryParams: { created: '1' } });
      } else {
        this.employee.set(result);
        this.form.patchValue(result);
        this.form.markAsPristine();
        this.success.set('Changes saved. Salary reports now use this updated record.');
      }
    } catch (error) {
      this.showError(error);
    } finally {
      this.busy.set(false);
    }
  }
  async changeStatus(): Promise<void> {
    const employee = this.employee();
    if (!employee || this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    this.success.set('');
    try {
      const result = await firstValueFrom(this.api.status(employee, !employee.active));
      this.employee.set(result);
      this.success.set(
        result.active
          ? 'Employee reactivated and included in active reports.'
          : 'Employee deactivated. The record is retained and excluded from active reports.',
      );
    } catch (error) {
      this.showError(error);
    } finally {
      this.busy.set(false);
    }
  }
}
