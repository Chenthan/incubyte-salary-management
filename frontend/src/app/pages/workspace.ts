import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, EMPTY, forkJoin, Subject, switchMap } from 'rxjs';
import { Api, problem } from '../core/api';
import { DepartmentCost, EmployeePage, Query, ReferenceData, Summary } from '../core/models';
import { MoneyPipe } from '../core/money.pipe';

@Component({
  selector: 'app-workspace',
  imports: [ReactiveFormsModule, RouterLink, DecimalPipe, DatePipe, MoneyPipe],
  templateUrl: './workspace.html',
  styleUrl: './workspace.scss',
})
export class Workspace implements OnInit {
  private readonly api = inject(Api);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroy = inject(DestroyRef);
  private readonly loadRequests = new Subject<Query>();
  readonly overview = this.route.snapshot.data['view'] === 'overview';
  readonly reference = signal<ReferenceData | null>(null);
  readonly report = signal<Summary | null>(null);
  readonly employees = signal<EmployeePage | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly query = signal<Query>({});
  readonly usd = computed(() => this.query()['usd'] !== false);
  readonly comparable = computed(() => this.usd() || Boolean(this.query()['currency']));
  readonly statistics = computed(() =>
    this.usd()
      ? this.report()?.usdEstimate
      : this.report()?.nativeGroups.length === 1
        ? this.report()?.nativeGroups[0]
        : null,
  );
  readonly filters = inject(FormBuilder).nonNullable.group({
    q: '',
    country: '',
    department: '',
    status: 'ACTIVE',
    currency: '',
    minSalary: '',
    mode: 'usd',
    sort: 'name',
    direction: 'asc',
  });

  ngOnInit(): void {
    this.api
      .referenceData()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (value) => this.reference.set(value),
        error: (error) => this.error.set(problem(error).detail ?? 'Could not load filters.'),
      });
    this.loadRequests
      .pipe(
        takeUntilDestroyed(this.destroy),
        switchMap((query) => {
          this.loading.set(true);
          this.error.set('');
          this.report.set(null);
          this.employees.set(null);
          return forkJoin({
            report: this.api.summary(query),
            employees: this.api.employees(query),
          }).pipe(
            catchError((error) => {
              this.error.set(problem(error).detail ?? 'Could not load this population.');
              this.loading.set(false);
              return EMPTY;
            }),
          );
        }),
      )
      .subscribe((value) => {
        this.report.set(value.report);
        this.employees.set(value.employees);
        this.loading.set(false);
      });
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroy)).subscribe((params) => {
      const mode = params.get('usd') === 'false' ? 'native' : 'usd';
      this.filters.patchValue({
        q: params.get('q') ?? '',
        country: params.get('country') ?? '',
        department: params.get('department') ?? '',
        status: params.get('status') ?? 'ACTIVE',
        currency: params.get('currency') ?? '',
        minSalary: params.get('minSalary') ?? '',
        mode,
        sort: params.get('sort') ?? 'name',
        direction: params.get('direction') ?? 'asc',
      });
      const pageValue = Number(params.get('page') ?? 0);
      const page = Number.isInteger(pageValue) && pageValue >= 0 ? pageValue : 0;
      const sizeValue = Number(params.get('size') ?? 25);
      const size = this.overview ? 5 : [10, 25, 50, 100].includes(sizeValue) ? sizeValue : 25;
      const query: Query = {
        ...this.population(),
        sort: this.filters.controls.sort.value,
        direction: this.filters.controls.direction.value,
        page,
        size,
      };
      this.query.set(query);
      this.loadRequests.next(query);
    });
  }

  private population(): Query {
    const values = this.filters.getRawValue();
    const query: Query = { usd: values.mode === 'usd', status: values.status };
    for (const key of ['q', 'country', 'department', 'currency', 'minSalary'] as const)
      if (values[key].trim()) query[key] = values[key].trim();
    return query;
  }
  apply(): void {
    const values = this.filters.getRawValue();
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        ...this.population(),
        sort: values.sort,
        direction: values.direction,
        page: 0,
        size: this.overview ? 5 : (this.query()['size'] ?? 25),
      },
    });
  }
  reset(): void {
    this.filters.reset({
      q: '',
      country: '',
      department: '',
      status: 'ACTIVE',
      currency: '',
      minSalary: '',
      mode: 'usd',
      sort: 'name',
      direction: 'asc',
    });
    this.apply();
  }
  retry(): void {
    this.loadRequests.next(this.query());
  }
  page(page: number): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { ...this.query(), page },
    });
  }
  changeSize(event: Event): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { ...this.query(), size: (event.target as HTMLSelectElement).value, page: 0 },
    });
  }
  listQuery(): Query {
    return { ...this.query(), page: 0, size: 25 };
  }
  departmentQuery(code: string): Query {
    return { ...this.listQuery(), department: code };
  }
  barWidth(item: DepartmentCost): number {
    const comparable =
      this.report()?.departments.filter((row) => row.currency === item.currency) ?? [];
    const maximum = Math.max(1, ...comparable.map((row) => Number(row.total)));
    return Math.max(0.5, (Number(item.total) / maximum) * 100);
  }
}
