
export class User{

  public id: number;
  public name: string;
  public mobileNumber: string;
  public email : string;
  public role : string;
  public authStatus : string;


  constructor(id?: number,
              name?: string,
              mobileNumber?: string,
              email?: string,
              role?: string,
              authStatus?:string){
    this.id = id || 0;
    this.name = name || '';
    this.mobileNumber = mobileNumber || '';
    this.email = email || '';
    this.role = role || '';
    this.authStatus = authStatus || '';
  }

}
