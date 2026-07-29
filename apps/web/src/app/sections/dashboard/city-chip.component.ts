import { Component, OnInit, inject, output, signal } from '@angular/core';
import { CityOption, DashboardBffControllerService } from 'shared-data-access';

/**
 * The dashboard scope chip in the section topnav (Slice 17). A Sant sees an
 * interactive City picker — the universal-read exception lets them read any City
 * in the State, and the City they pick is persisted as their default. Every other
 * role sees a non-interactive scope indicator instead, because their view is
 * fixed to their assignments server-side. Picking a City persists it via the BFF
 * and announces {@link cityPicked} so the shell re-reads the sections.
 */
@Component({
  selector: 'app-city-chip',
  standalone: true,
  templateUrl: './city-chip.component.html',
  styleUrl: './city-chip.component.scss',
})
export class CityChipComponent implements OnInit {
  private readonly api = inject(DashboardBffControllerService);

  readonly sant = signal(false);
  readonly cities = signal<CityOption[]>([]);
  readonly selectedCityId = signal<string | null>(null);

  /** Emitted once the scope loads, so the shell can prompt a Sant who has no City yet. */
  readonly scopeLoaded = output<{ sant: boolean; selectedCityId: string | null }>();

  /** Emitted after a pick is persisted, carrying the newly selected City id. */
  readonly cityPicked = output<string>();

  ngOnInit(): void {
    this.api.scope().subscribe((scope) => {
      this.sant.set(scope.sant);
      this.cities.set(scope.cities);
      this.selectedCityId.set(scope.selectedCityId);
      this.scopeLoaded.emit({ sant: scope.sant, selectedCityId: scope.selectedCityId });
    });
  }

  choose(cityId: string): void {
    this.api.chooseCity({ cityId }).subscribe(() => {
      this.selectedCityId.set(cityId);
      this.cityPicked.emit(cityId);
    });
  }
}
