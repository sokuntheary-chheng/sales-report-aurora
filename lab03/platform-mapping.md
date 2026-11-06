# Lab-03 · Challenge: would the same design carry over? (platform mapping)

The challenge asks whether the `ReportViewModel` design would work in the
team's planned **Android** app (Jetpack) and in the finance team's **WPF**
tools. Every member of our ViewModel is mapped below; the two columns are
what that member would be made of on each platform.

## Member-by-member mapping

| `ReportViewModel` member | JavaFX (this lab) | Android · Jetpack | WPF |
|---|---|---|---|
| `month` (input) | `ObjectProperty<YearMonth>`, two-way bound | `MutableStateFlow<YearMonth?>` in the `ViewModel`, collected in the composable and written back through an `onValueChange` callback | `INotifyPropertyChanged` property `Month` (`DateTimePicker`/`ComboBox` two-way bound) |
| `branchSelected("PNH"/"REP"/"BTB")` (input) | one `BooleanProperty` per branch | one `MutableStateFlow<Boolean>` per branch, or a `Map<String, Boolean>` state | three `bool` properties, each bound to a `CheckBox` |
| `productFilter` (input) | `StringProperty` | `MutableStateFlow<String>` updated by the text field's `onValueChange` | `string` property bound to a `TextBox` |
| `monthChoices` (output) | `ObservableList<YearMonth>` (unmodifiable) | `StateFlow<List<YearMonth>>`; the list is computed once in the `ViewModel` | `ObservableCollection<YearMonth>` or a plain list property (the `ComboBox` binds to it) |
| `summaries` (output) | `ObservableList<BranchSummary>` | `StateFlow<List<BranchSummary>>` (immutable list, replaced on load) | `ObservableCollection<BranchSummary>` bound to the `DataGrid`/`ItemsControl` |
| `visibleProducts` (output) | `FilteredList<ProductTotal>` with a bound predicate | derived flow: `combine(products, filter) { list, q -> list.filter(...) }` collected by the UI | `ICollectionView`/`CollectionViewSource.View` with a `Filter` delegate (WPF's own filtered view) |
| `loading` (output) | `ReadOnlyBooleanProperty` | `MutableStateFlow<Boolean>` | `bool` property + `INotifyPropertyChanged`; drives `ProgressBar.IsIndeterminate` |
| `progress` (output) | `ReadOnlyDoubleProperty` | `MutableStateFlow<Double>` (or `Flow<Double>` from the loader) | `double` property bound to `ProgressBar.Value` |
| `status` (output) | `ReadOnlyStringProperty` | `MutableStateFlow<String>` | `string` property bound to a `TextBlock` |
| `loadError` (output) | `ReadOnlyStringProperty` | `MutableStateFlow<String?>` | `string` property, bound to an error `TextBlock` |
| `monthError`, `branchError` (computed) | `StringExpression` bindings over `month` / branch flags | derived: `combine(month, flags) { ... }` producing the message | computed read-only properties that raise `PropertyChanged` when the inputs change |
| `validationMessage` (computed) | `StringBinding` joining the two messages | derived flow combining `monthError` and `branchError` | computed `string` property, re-evaluated in the inputs' `PropertyChanged` handlers |
| `valid` (computed) | `ObservableBooleanValue` (`monthError`/`branchError` empty) | `derived StateFlow<Boolean>` | computed `bool` property |
| `canGenerate` (computed) | `ObservableBooleanValue` = `valid and not loading` | `combine(valid, loading) { v, l -> v && !l }` | computed `bool` used by `ICommand.CanExecute` |
| `totalRevenue` (computed) | `ObjectBinding<BigDecimal>` summing `summaries` | `summaries.map { it.sumOf { s -> s.revenue } }` as a `StateFlow<BigDecimal>` | computed `decimal` property summing the loaded collection |
| `totalRevenueText` (computed) | `StringBinding` (`"USD …"`, `Locale.US`) | derived flow formatting `totalRevenue` (`NumberFormat`) | computed `string` property formatting the decimal |
| `generate` (command) | `DelegateCommand(this::generate, canGenerate)` | a `fun generate()` on the `ViewModel` launched in `viewModelScope`; the button's enabled state comes from `canGenerate` | `ICommand` (`RelayCommand`) with `CanExecute` = `canGenerate`, `Execute` = `async` method |
| `cancel` (command) | `DelegateCommand(this::cancel, loading)` | `fun cancel()` cancelling the loader `Job` (`job?.cancel()`); enabled while `loading` | `ICommand` cancelling a `CancellationTokenSource`; enabled while loading |
| `SummarySource` port | lambda in `ReportViewerApp` over `CsvMonthReader` + `DefaultSalesAggregator` | repository function `suspend fun loadBranch(month, branch): BranchSummary` read inside `viewModelScope.launch(Dispatchers.IO)` | `async Task<BranchSummary>` method in a repository class, awaited by the ViewModel |
| `BackgroundJobs` port | `CallerThreadJobs` / `FxBackgroundJobs` (`javafx.concurrent.Task`) | `viewModelScope.launch` + `Flow` for progress, cancellation through `Job.cancel()` — no port needed, the coroutine *is* the port | `async/await` + `IProgress<T>` for progress, `CancellationToken` for cancellation; the UI thread is resumed by `await` |
| `Command` port | interface with `executableProperty()` | not needed: the `ViewModel` exposes functions + `StateFlow<Boolean>` enabled flags (unidirectional events) | `ICommand` is part of the platform; `DelegateCommand` becomes a `RelayCommand` |

## What would not carry over (5 sentences)

1. **Two-way binding is the big one:** JavaFX's `bindBidirectional` between
   `month` and the month box has no direct Jetpack equivalent — Compose is
   strictly unidirectional (state in the `ViewModel`, `onValueChange`
   callbacks writing back), while WPF keeps real two-way binding and would
   map almost one-to-one.
2. **Lifetime across rotation:** an Android `ViewModel` survives a
   configuration change but is cleared when the process dies, so anything
   the screen must restore needs `SavedStateHandle`; JavaFX and WPF have no
   rotation concept — the window's lifetime *is* the ViewModel's lifetime,
   which is a different contract to code against.
3. **Observable collections:** JavaFX's live `FilteredList` over an
   `ObservableList` has no twin in Compose (lists are immutable and the
   filtering is recomposed), and WPF's `ObservableCollection` would work
   but its `ICollectionView` filtering is WPF-specific machinery.
4. **Threading discipline:** this lab's rule "worker threads never touch a
   property, the listener runs on the FX thread" becomes "coroutines on
   `viewModelScope`, state updated on the main dispatcher" on Android and
   "`async`/`await` resuming on the `Dispatcher`" on WPF — same idea,
   completely different API surface, and none of the `BackgroundJobs` port
   code is reusable as written.
5. **Commands and controls:** `DelegateCommand` with an observable
   executable flag maps naturally to WPF's `ICommand.CanExecute` but has no
   Jetpack counterpart (enabled-state is just another piece of state
   composed with the button), and every binding target —
   `progressProperty()`, `disableProperty()`, skins — is a JavaFX control
   API that neither platform shares.

## How this was checked against this lab's code

- The dashboard (`DashboardView`) binds **only** to `ReportViewModel`
  members listed above — no view logic, no second state holder.
- `git diff lab-03-task5 -- src/main/java/edu/itc/salesreport/ui/ReportViewModel.java`
  is empty: the ViewModel was not touched for the challenge (tag
  `lab-03-task5` on the Task 5 commit `dfc3e5c`).
- `ReportViewModelTest.twoViewsOnTheSameViewModelBothSeeTheTotal` shows
  two listeners on `totalRevenueTextProperty()` both receiving
  `"USD 600.00"` after one load — the property is the same source both
  windows read.
