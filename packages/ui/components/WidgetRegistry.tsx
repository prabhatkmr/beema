import { WidgetType } from '../types/layout';
import { TextInputWidget } from './widgets/TextInputWidget';
import { NumberInputWidget } from './widgets/NumberInputWidget';
import { SelectWidget } from './widgets/SelectWidget';
import { DatePickerWidget } from './widgets/DatePickerWidget';
import { CheckboxWidget } from './widgets/CheckboxWidget';

export const WidgetRegistry: Record<WidgetType, React.ComponentType<any>> = {
  TEXT_INPUT: TextInputWidget,
  NUMBER_INPUT: NumberInputWidget,
  CURRENCY_INPUT: NumberInputWidget, // Reuse NumberInput with formatting
  PERCENTAGE_INPUT: NumberInputWidget,
  DATE_PICKER: DatePickerWidget,
  SELECT: SelectWidget,
  CHECKBOX: CheckboxWidget,
  RADIO_GROUP: SelectWidget, // Simplified for now
  TEXTAREA: TextInputWidget, // Can be enhanced
  FILE_UPLOAD: TextInputWidget, // Placeholder
  SWITCH: CheckboxWidget,
  SLIDER: NumberInputWidget,
  RICH_TEXT: TextInputWidget, // Placeholder
};

export function getWidgetComponent(widgetType: WidgetType) {
  const Component = WidgetRegistry[widgetType];

  if (!Component) {
    console.warn(`Widget type "${widgetType}" not found in registry`);
    return TextInputWidget; // Fallback to text input
  }

  return Component;
}
