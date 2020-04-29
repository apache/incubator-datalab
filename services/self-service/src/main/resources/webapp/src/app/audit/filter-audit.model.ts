export class FilterAuditModel {

  static getDefault(): FilterAuditModel {
    return new FilterAuditModel([], [], '', '');
  }

  constructor(
    public users: Array<string>,
    public actions: Array<string>,
    public date_start: string,
    public date_end: string,
  ) { }

  defaultConfigurations(): void {
    this.users = [];
    this.actions = [];
    this.date_start = '';
    this.date_end = '';

  }
}
