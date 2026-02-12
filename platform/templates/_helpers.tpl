{{/*
Expand the name of the chart.
*/}}
{{- define "beema.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "beema.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "beema.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "beema.labels" -}}
helm.sh/chart: {{ include "beema.chart" . }}
{{ include "beema.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "beema.selectorLabels" -}}
app.kubernetes.io/name: {{ include "beema.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "beema.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "beema.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
PostgreSQL connection string
*/}}
{{- define "beema.postgresql.fullname" -}}
{{- if .Values.postgresql.enabled }}
{{- printf "%s-postgresql" (include "beema.fullname" .) }}
{{- else }}
{{- .Values.postgresql.external.host }}
{{- end }}
{{- end }}

{{/*
Kafka bootstrap servers
*/}}
{{- define "beema.kafka.bootstrapServers" -}}
{{- if .Values.kafka.enabled }}
{{- printf "%s-kafka:9092" (include "beema.fullname" .) }}
{{- else }}
{{- .Values.kafka.external.bootstrapServers }}
{{- end }}
{{- end }}

{{/*
Temporal server address
*/}}
{{- define "beema.temporal.address" -}}
{{- if .Values.temporal.server.enabled }}
{{- printf "%s-temporal-frontend.%s.svc.cluster.local:%d" (include "beema.fullname" .) .Release.Namespace (.Values.temporal.server.port | int) }}
{{- else }}
{{- printf "%s:%d" .Values.temporal.server.host (.Values.temporal.server.port | int) }}
{{- end }}
{{- end }}

{{/*
Return the proper Docker Image Registry Secret Names
*/}}
{{- define "beema.imagePullSecrets" -}}
{{- if .Values.global.imageRegistry }}
{{- if .Values.global.imagePullSecrets }}
imagePullSecrets:
{{- range .Values.global.imagePullSecrets }}
  - name: {{ . }}
{{- end }}
{{- end }}
{{- end }}
{{- end }}
