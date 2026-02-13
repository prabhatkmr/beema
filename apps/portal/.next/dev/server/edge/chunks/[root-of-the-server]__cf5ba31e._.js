(globalThis.TURBOPACK || (globalThis.TURBOPACK = [])).push(["chunks/[root-of-the-server]__cf5ba31e._.js",
"[externals]/node:buffer [external] (node:buffer, cjs)", ((__turbopack_context__, module, exports) => {

const mod = __turbopack_context__.x("node:buffer", () => require("node:buffer"));

module.exports = mod;
}),
"[externals]/node:async_hooks [external] (node:async_hooks, cjs)", ((__turbopack_context__, module, exports) => {

const mod = __turbopack_context__.x("node:async_hooks", () => require("node:async_hooks"));

module.exports = mod;
}),
"[project]/apps/portal/locales/en-GB.json (json)", ((__turbopack_context__) => {

__turbopack_context__.v({"dashboard":{"greeting":"Good Morning, {name}","welcome":"Welcome to your Beema workspace","title":"Dashboard"},"apps":{"policy":{"title":"Beema Policy","description":"Manage insurance policies","pending":"{count} pending"},"claim":{"title":"Beema Claim","description":"Submit and track claims","pending":"{count} pending"},"billing":{"title":"Beema Billing","description":"Invoices and payments","pending":"{count} pending"},"studio":{"title":"Beema Studio","description":"Product configuration","pending":"{count} pending"},"agents":{"title":"Beema Agents","description":"Agent management","pending":"{count} pending"},"analytics":{"title":"Beema Analytics","description":"Reports and insights","pending":"{count} pending"},"profile":{"title":"Profile","description":"Manage your profile","pending":"{count} pending"},"settings":{"title":"Settings","description":"System settings","pending":"{count} pending"}},"common":{"loading":"Loading...","error":"An error occurred","save":"Save","cancel":"Cancel","delete":"Delete","edit":"Edit","create":"Create","search":"Search","filter":"Filter","export":"Export","import":"Import"}});}),
"[project]/apps/portal/locales/en-US.json (json)", ((__turbopack_context__) => {

__turbopack_context__.v({"dashboard":{"greeting":"Good Morning, {name}","welcome":"Welcome to your Beema workspace","title":"Dashboard"},"apps":{"policy":{"title":"Beema Policy","description":"Manage insurance policies","pending":"{count} pending"},"claim":{"title":"Beema Claim","description":"Submit and track claims","pending":"{count} pending"},"billing":{"title":"Beema Billing","description":"Invoices and payments","pending":"{count} pending"},"studio":{"title":"Beema Studio","description":"Product configuration","pending":"{count} pending"},"agents":{"title":"Beema Agents","description":"Agent management","pending":"{count} pending"},"analytics":{"title":"Beema Analytics","description":"Reports and insights","pending":"{count} pending"},"profile":{"title":"Profile","description":"Manage your profile","pending":"{count} pending"},"settings":{"title":"Settings","description":"System settings","pending":"{count} pending"}},"common":{"loading":"Loading...","error":"An error occurred","save":"Save","cancel":"Cancel","delete":"Delete","edit":"Edit","create":"Create","search":"Search","filter":"Filter","export":"Export","import":"Import"}});}),
"[project]/apps/portal/locales/es-ES.json (json)", ((__turbopack_context__) => {

__turbopack_context__.v({"dashboard":{"greeting":"Buenos Días, {name}","welcome":"Bienvenido a tu espacio de trabajo Beema","title":"Panel de Control"},"apps":{"policy":{"title":"Pólizas Beema","description":"Gestionar pólizas de seguros","pending":"{count} pendientes"},"claim":{"title":"Reclamaciones Beema","description":"Enviar y rastrear reclamaciones","pending":"{count} pendientes"},"billing":{"title":"Facturación Beema","description":"Facturas y pagos","pending":"{count} pendientes"},"studio":{"title":"Estudio Beema","description":"Configuración de productos","pending":"{count} pendientes"},"agents":{"title":"Agentes Beema","description":"Gestión de agentes","pending":"{count} pendientes"},"analytics":{"title":"Analíticas Beema","description":"Informes y estadísticas","pending":"{count} pendientes"},"profile":{"title":"Perfil","description":"Gestionar tu perfil","pending":"{count} pendientes"},"settings":{"title":"Configuración","description":"Ajustes del sistema","pending":"{count} pendientes"}},"common":{"loading":"Cargando...","error":"Ha ocurrido un error","save":"Guardar","cancel":"Cancelar","delete":"Eliminar","edit":"Editar","create":"Crear","search":"Buscar","filter":"Filtrar","export":"Exportar","import":"Importar"}});}),
"[project]/apps/portal/i18n.ts [middleware-edge] (ecmascript)", ((__turbopack_context__) => {
"use strict";

__turbopack_context__.s([
    "default",
    ()=>__TURBOPACK__default__export__,
    "locales",
    ()=>locales
]);
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f2e$pnpm$2f$next$2d$intl$40$4$2e$8$2e$2_next$40$16$2e$1$2e$6_react$40$19$2e$2$2e$3_typescript$40$5$2e$9$2e$3$2f$node_modules$2f$next$2d$intl$2f$dist$2f$esm$2f$development$2f$server$2f$react$2d$server$2f$getRequestConfig$2e$js__$5b$middleware$2d$edge$5d$__$28$ecmascript$29$__$3c$export__default__as__getRequestConfig$3e$__ = __turbopack_context__.i("[project]/node_modules/.pnpm/next-intl@4.8.2_next@16.1.6_react@19.2.3_typescript@5.9.3/node_modules/next-intl/dist/esm/development/server/react-server/getRequestConfig.js [middleware-edge] (ecmascript) <export default as getRequestConfig>");
;
const locales = [
    'en-GB',
    'en-US',
    'es-ES'
];
const __TURBOPACK__default__export__ = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f2e$pnpm$2f$next$2d$intl$40$4$2e$8$2e$2_next$40$16$2e$1$2e$6_react$40$19$2e$2$2e$3_typescript$40$5$2e$9$2e$3$2f$node_modules$2f$next$2d$intl$2f$dist$2f$esm$2f$development$2f$server$2f$react$2d$server$2f$getRequestConfig$2e$js__$5b$middleware$2d$edge$5d$__$28$ecmascript$29$__$3c$export__default__as__getRequestConfig$3e$__["getRequestConfig"])(async ({ requestLocale })=>{
    // requestLocale is the locale from the request (URL, cookie, header, etc.)
    let locale = await requestLocale;
    // Validate that the incoming `locale` parameter is valid
    if (!locale || !locales.includes(locale)) {
        locale = 'en-GB';
    }
    return {
        locale,
        messages: (await __turbopack_context__.f({
            "./locales/en-GB.json": {
                id: ()=>"[project]/apps/portal/locales/en-GB.json (json)",
                module: ()=>Promise.resolve().then(()=>__turbopack_context__.i("[project]/apps/portal/locales/en-GB.json (json)"))
            },
            "./locales/en-US.json": {
                id: ()=>"[project]/apps/portal/locales/en-US.json (json)",
                module: ()=>Promise.resolve().then(()=>__turbopack_context__.i("[project]/apps/portal/locales/en-US.json (json)"))
            },
            "./locales/es-ES.json": {
                id: ()=>"[project]/apps/portal/locales/es-ES.json (json)",
                module: ()=>Promise.resolve().then(()=>__turbopack_context__.i("[project]/apps/portal/locales/es-ES.json (json)"))
            }
        }).import(`./locales/${locale}.json`)).default,
        // Configure locale-specific formatting
        timeZone: locale === 'en-US' ? 'America/New_York' : locale === 'es-ES' ? 'Europe/Madrid' : 'Europe/London',
        now: new Date()
    };
});
}),
"[project]/apps/portal/middleware.ts [middleware-edge] (ecmascript)", ((__turbopack_context__) => {
"use strict";

__turbopack_context__.s([
    "config",
    ()=>config,
    "default",
    ()=>__TURBOPACK__default__export__
]);
var __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f2e$pnpm$2f$next$2d$intl$40$4$2e$8$2e$2_next$40$16$2e$1$2e$6_react$40$19$2e$2$2e$3_typescript$40$5$2e$9$2e$3$2f$node_modules$2f$next$2d$intl$2f$dist$2f$esm$2f$development$2f$middleware$2f$middleware$2e$js__$5b$middleware$2d$edge$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/node_modules/.pnpm/next-intl@4.8.2_next@16.1.6_react@19.2.3_typescript@5.9.3/node_modules/next-intl/dist/esm/development/middleware/middleware.js [middleware-edge] (ecmascript)");
var __TURBOPACK__imported__module__$5b$project$5d2f$apps$2f$portal$2f$i18n$2e$ts__$5b$middleware$2d$edge$5d$__$28$ecmascript$29$__ = __turbopack_context__.i("[project]/apps/portal/i18n.ts [middleware-edge] (ecmascript)");
;
;
const __TURBOPACK__default__export__ = (0, __TURBOPACK__imported__module__$5b$project$5d2f$node_modules$2f2e$pnpm$2f$next$2d$intl$40$4$2e$8$2e$2_next$40$16$2e$1$2e$6_react$40$19$2e$2$2e$3_typescript$40$5$2e$9$2e$3$2f$node_modules$2f$next$2d$intl$2f$dist$2f$esm$2f$development$2f$middleware$2f$middleware$2e$js__$5b$middleware$2d$edge$5d$__$28$ecmascript$29$__["default"])({
    // A list of all locales that are supported
    locales: __TURBOPACK__imported__module__$5b$project$5d2f$apps$2f$portal$2f$i18n$2e$ts__$5b$middleware$2d$edge$5d$__$28$ecmascript$29$__["locales"],
    // Used when no locale matches
    defaultLocale: 'en-GB',
    // Always show locale in URL for clarity
    localePrefix: 'always'
});
const config = {
    // Match all pathnames except for
    // - api routes
    // - _next (Next.js internals)
    // - _vercel (Vercel internals)
    // - files with extensions (e.g. .png, .css)
    matcher: [
        '/',
        '/((?!api|_next|_vercel|.*\\..*).*)'
    ]
};
}),
]);

//# sourceMappingURL=%5Broot-of-the-server%5D__cf5ba31e._.js.map