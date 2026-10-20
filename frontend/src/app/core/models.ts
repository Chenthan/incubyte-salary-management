export interface Identity {
  username: string;
}
export interface Option {
  code: string;
  label: string;
}
export interface Rate {
  currency: string;
  usdPerUnit: string;
  version: string;
  effectiveDate: string;
}
export interface ReferenceData {
  countries: Option[];
  departments: Option[];
  rates: Rate[];
}
export interface Employee {
  id: string;
  employeeCode: string;
  firstName: string;
  lastName: string;
  email: string;
  countryCode: string;
  countryName: string;
  departmentCode: string;
  departmentName: string;
  annualSalary: string;
  currencyCode: string;
  usdEstimate: string;
  active: boolean;
  version: number;
  updatedAt: string;
}
export interface EmployeeInput {
  employeeCode: string;
  firstName: string;
  lastName: string;
  email: string;
  countryCode: string;
  departmentCode: string;
  annualSalary: string;
  currencyCode: string;
  version?: number;
}
export interface EmployeePage {
  content: Employee[];
  totalElements: number;
  page: number;
  size: number;
  totalPages: number;
}
export interface Statistics {
  currency: string;
  count: number;
  total: string;
  average: string | null;
  median: string | null;
  minimum: string | null;
  maximum: string | null;
}
export interface DepartmentCost {
  code: string;
  name: string;
  currency: string;
  count: number;
  total: string;
}
export interface Summary {
  headcount: number;
  nativeGroups: Statistics[];
  usdEstimate: Statistics;
  departments: DepartmentCost[];
  rates: Rate[];
  estimateLabel: string;
  asOf: string;
}
export interface ApiProblem {
  status?: number;
  code?: string;
  detail?: string;
  fieldErrors?: Record<string, string>;
}
export type Query = Record<string, string | number | boolean>;
