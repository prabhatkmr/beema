import Link from 'next/link';
import { Button, Card, CardHeader, CardTitle, CardContent } from '@beema/ui';

export default function Home() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center p-24">
      <div className="z-10 max-w-5xl w-full items-center justify-center">
        <h1 className="text-4xl font-bold text-center mb-8">
          Beema Studio
        </h1>
        <p className="text-center mb-8 text-gray-600">
          Visual layout builder for insurance forms
        </p>

        <div className="grid grid-cols-2 gap-4 max-w-2xl mx-auto">
          <Card>
            <CardHeader>
              <CardTitle>Canvas Builder</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-gray-600 mb-4">
                Drag and drop fields to create form layouts
              </p>
              <Link href="/canvas">
                <Button variant="primary" className="w-full">
                  Open Canvas
                </Button>
              </Link>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Blueprints</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-gray-600 mb-4">
                Manage message transformation blueprints
              </p>
              <Link href="/blueprints">
                <Button variant="secondary" className="w-full">
                  View Blueprints
                </Button>
              </Link>
            </CardContent>
          </Card>
        </div>
      </div>
    </main>
  );
}
