"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Separator } from "@/components/ui/separator";
import type { Layout, Field } from "@/types/layout";

export interface LayoutRendererProps {
  layout: Layout;
  data: Record<string, any>;
  onChange: (fieldId: string, value: any) => void;
}

function gridColsClass(columns: number): string {
  const map: Record<number, string> = {
    1: "grid-cols-1",
    2: "grid-cols-1 md:grid-cols-2",
    3: "grid-cols-1 md:grid-cols-2 lg:grid-cols-3",
    4: "grid-cols-1 md:grid-cols-2 lg:grid-cols-4",
  };
  return map[columns] ?? "grid-cols-1";
}

function FieldRenderer({
  field,
  value,
  onChange,
}: {
  field: Field;
  value: any;
  onChange: (fieldId: string, value: any) => void;
}) {
  switch (field.type) {
    case "TEXT":
      return (
        <div className="space-y-2">
          <Label htmlFor={field.id}>
            {field.label}
            {field.required && <span className="ml-1 text-destructive">*</span>}
          </Label>
          <Input
            id={field.id}
            placeholder={field.placeholder}
            value={value ?? field.defaultValue ?? ""}
            onChange={(e) => onChange(field.id, e.target.value)}
            required={field.required}
          />
        </div>
      );

    case "CURRENCY":
      return (
        <div className="space-y-2">
          <Label htmlFor={field.id}>
            {field.label}
            {field.required && <span className="ml-1 text-destructive">*</span>}
          </Label>
          <div className="relative">
            <span className="absolute left-3 top-1/2 -translate-y-1/2 text-sm text-muted-foreground">
              $
            </span>
            <Input
              id={field.id}
              type="number"
              className="pl-7"
              placeholder={field.placeholder ?? "0.00"}
              value={value ?? field.defaultValue ?? ""}
              onChange={(e) => onChange(field.id, e.target.value)}
              required={field.required}
              min={0}
              step="0.01"
            />
          </div>
        </div>
      );

    case "SELECT":
      return (
        <div className="space-y-2">
          <Label htmlFor={field.id}>
            {field.label}
            {field.required && <span className="ml-1 text-destructive">*</span>}
          </Label>
          <Select
            value={value ?? field.defaultValue ?? ""}
            onValueChange={(v) => onChange(field.id, v)}
          >
            <SelectTrigger id={field.id}>
              <SelectValue placeholder={field.placeholder ?? "Select..."} />
            </SelectTrigger>
            <SelectContent>
              {(field.options ?? []).map((option) => (
                <SelectItem key={option} value={option}>
                  {option}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      );

    case "TOGGLE":
      return (
        <div className="flex items-center space-x-2 pt-6">
          <Checkbox
            id={field.id}
            checked={value ?? field.defaultValue ?? false}
            onCheckedChange={(checked) => onChange(field.id, checked)}
          />
          <Label htmlFor={field.id} className="cursor-pointer">
            {field.label}
            {field.required && <span className="ml-1 text-destructive">*</span>}
          </Label>
        </div>
      );

    default:
      return null;
  }
}

export function LayoutRenderer({ layout, data, onChange }: LayoutRendererProps) {
  return (
    <div className="space-y-6">
      {layout.regions.map((region, index) => (
        <div key={region.id}>
          {index > 0 && <Separator className="mb-6" />}
          <Card>
            <CardHeader>
              <CardTitle>{region.label}</CardTitle>
            </CardHeader>
            <CardContent>
              <div className={`grid gap-4 ${gridColsClass(region.columns)}`}>
                {region.fields.map((field) => (
                  <FieldRenderer
                    key={field.id}
                    field={field}
                    value={data[field.id]}
                    onChange={onChange}
                  />
                ))}
              </div>
            </CardContent>
          </Card>
        </div>
      ))}
    </div>
  );
}
