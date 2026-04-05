export interface EnrollPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
