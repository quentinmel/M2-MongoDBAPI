export interface ConvocationFile {
  blob: Blob;
  filename: string;
}

const errorMessages: Record<number, string> = {
  401: 'Connectez-vous pour accéder à cette convocation.',
  404: 'Cette convocation est introuvable ou ne vous appartient pas.',
  409: 'Une convocation ne peut pas être générée pour un rendez-vous annulé.',
  500: 'Le service n’a pas pu générer la convocation. Réessayez plus tard.',
};

function safePdfFilename(value: string, fallback: string): string {
  const basename = value.replaceAll('\\', '/').split('/').pop()?.replace(/[\u0000-\u001f\u007f]/g, '').trim();
  const filename = basename || fallback;
  return /\.pdf$/i.test(filename) ? filename : `${filename}.pdf`;
}

export function filenameFrom(contentDisposition: string | null, fallback = 'convocation.pdf'): string {
  if (!contentDisposition) return safePdfFilename(fallback, 'convocation.pdf');

  const encoded = contentDisposition.match(/filename\*\s*=\s*UTF-8''([^;]+)/i)?.[1]?.trim();
  const plain = contentDisposition.match(/filename\s*=\s*(?:"([^"]*)"|([^;]*))/i);
  let filename = encoded ?? plain?.[1] ?? plain?.[2]?.trim();
  if (filename && encoded) {
    try {
      filename = decodeURIComponent(filename);
    } catch {
      filename = undefined;
    }
  }
  return safePdfFilename(filename || fallback, fallback);
}

export async function fetchConvocation(
  appointmentId: number,
  request: typeof fetch = fetch,
): Promise<ConvocationFile> {
  if (!Number.isSafeInteger(appointmentId) || appointmentId <= 0) {
    throw new Error('Identifiant de rendez-vous invalide.');
  }

  const response = await request(`/api/appointments/${appointmentId}/convocation.pdf`, {
    credentials: 'same-origin',
    headers: {Accept: 'application/pdf'},
  });
  if (!response.ok) {
    const message = errorMessages[response.status] || `Impossible de récupérer la convocation (erreur ${response.status}).`;
    throw new Error(message);
  }

  const contentType = response.headers.get('Content-Type')?.split(';', 1)[0].trim().toLowerCase();
  if (contentType !== 'application/pdf') {
    throw new Error('Le serveur n’a pas renvoyé un document PDF valide.');
  }

  return {
    blob: await response.blob(),
    filename: filenameFrom(
      response.headers.get('Content-Disposition'),
      `convocation-rdv-${appointmentId}.pdf`,
    ),
  };
}

export async function openConvocation(
  appointmentId: number,
  request: typeof fetch = fetch,
): Promise<void> {
  const popup = window.open('about:blank', '_blank');
  if (!popup) throw new Error('Autorisez les fenêtres contextuelles pour afficher la convocation.');

  try {
    const file = await fetchConvocation(appointmentId, request);
    const url = URL.createObjectURL(file.blob);
    popup.location.href = url;
    window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
  } catch (error) {
    popup.close();
    throw error;
  }
}

export async function saveConvocation(
  appointmentId: number,
  request: typeof fetch = fetch,
): Promise<void> {
  const file = await fetchConvocation(appointmentId, request);
  const url = URL.createObjectURL(file.blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = file.filename;
  link.style.display = 'none';
  document.body.append(link);
  link.click();
  link.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 1_000);
}
