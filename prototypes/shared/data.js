// PROTOTYPE — shared fake domain data, using the canonical vocabulary from CONTEXT.md.

window.__DATA = (function () {
  const roster = [
    { id: 'p1', name: 'Ravi Mehta', initials: 'RM', mobile: '98201 11122', dob: '2008-04-12', homeSabhaSame: true, lastMissed: 0 },
    { id: 'p2', name: 'Arjun Shah', initials: 'AS', mobile: '98201 22334', dob: '2009-01-08', homeSabhaSame: true, lastMissed: 1 },
    { id: 'p3', name: 'Harsh Joshi', initials: 'HJ', mobile: '98201 44556', dob: '2007-11-30', homeSabhaSame: true, lastMissed: 3, candidate: true },
    { id: 'p4', name: 'Kunal Trivedi', initials: 'KT', mobile: '98201 66778', dob: '2008-07-21', homeSabhaSame: true, lastMissed: 0 },
    { id: 'p5', name: 'Nirav Patel', initials: 'NP', mobile: '98201 88990', dob: '2009-03-14', homeSabhaSame: true, lastMissed: 4, candidate: true },
    { id: 'p6', name: 'Devansh Desai', initials: 'DD', mobile: '98201 12121', dob: '2007-09-02', homeSabhaSame: true, lastMissed: 0 },
    { id: 'p7', name: 'Yash Bhatt', initials: 'YB', mobile: '98201 23232', dob: '2008-12-18', homeSabhaSame: true, lastMissed: 2 },
    { id: 'p8', name: 'Manan Shukla', initials: 'MS', mobile: '98201 34343', dob: '2009-06-05', homeSabhaSame: true, lastMissed: 1 },
    { id: 'p9', name: 'Parth Vyas', initials: 'PV', mobile: '98201 45454', dob: '2008-02-22', homeSabhaSame: true, lastMissed: 5, candidate: true },
    { id: 'p10', name: 'Sahil Pandya', initials: 'SP', mobile: '98201 56565', dob: '2007-08-11', homeSabhaSame: true, lastMissed: 0 },
    { id: 'p11', name: 'Krish Kapadia', initials: 'KK', mobile: '98201 67676', dob: '2009-05-19', homeSabhaSame: true, lastMissed: 0 },
    { id: 'p12', name: 'Aarav Soni', initials: 'AS', mobile: '98201 78787', dob: '2008-10-09', homeSabhaSame: true, lastMissed: 1 },
    { id: 'p13', name: 'Mihir Gajjar', initials: 'MG', mobile: '98201 89898', dob: '2008-06-25', homeSabhaSame: true, lastMissed: 0, walkIn: true },
    { id: 'p14', name: 'Tanmay Raval', initials: 'TR', mobile: '98201 90909', dob: '2007-03-08', homeSabhaSame: true, lastMissed: 6, candidate: true },
    { id: 'p15', name: 'Shrey Modi', initials: 'SM', mobile: '98202 01010', dob: '2009-02-14', homeSabhaSame: true, lastMissed: 2 },
    { id: 'p16', name: 'Hetav Suthar', initials: 'HS', mobile: '98202 12121', dob: '2008-11-27', homeSabhaSame: true, lastMissed: 0 },
    { id: 'p17', name: 'Yug Thakkar', initials: 'YT', mobile: '98202 23232', dob: '2009-04-03', homeSabhaSame: true, lastMissed: 1 },
    { id: 'p18', name: 'Veer Acharya', initials: 'VA', mobile: '98202 34343', dob: '2008-08-15', homeSabhaSame: true, lastMissed: 0 },
  ];

  const sabha = {
    name: 'Yuvak Sabha',
    kshetra: 'Andheri-7',
    track: 'Regular',
    sanchalak: 'Pratik Patel',
    venue: 'Sansthan Hall, near Andheri Station',
    schedule: 'Sunday 7:00–8:30pm',
  };

  const occurrence = {
    date: 'Sun, 24 May 2026',
    state: 'Open for Marking',
    venueOverride: null,
  };

  const sync = {
    lastSyncAgo: '3 min',
    pendingActions: 0,
    rosterStaleDays: 0,
    online: true,
  };

  // Existing-Person stub for de-dup demo. Mobile-collision triggers when user types this.
  const dedupMatch = {
    id: 'pX',
    name: 'Ravi Mehta',
    initials: 'RM',
    mobile: '98201 11122',
    dob: '2008-04-12',
    homeSabha: 'Yuvak Sabha — Kshetra Andheri-7',
    addedOn: '12 Jan 2024',
    addedBy: 'Pratik Patel (Sanchalak)',
  };

  const tree = [
    {
      name: 'Mumbai West',
      type: 'Zone',
      candidates: 14,
      attendance: 0.71,
      children: [
        {
          name: 'Andheri-7',
          type: 'Kshetra',
          candidates: 6,
          attendance: 0.73,
          children: [
            { name: 'Yuvak Sabha', type: 'Sabha', kind: 'Regular', candidates: 3, attendance: 0.75, occurrences: 18 },
            { name: 'Baal Sabha', type: 'Sabha', kind: 'Regular', candidates: 2, attendance: 0.78, occurrences: 18 },
            { name: 'Sanyukta Sabha', type: 'Sabha', kind: 'Regular', candidates: 1, attendance: 0.66, occurrences: 18 },
            { name: 'BSS Baal Sabha', type: 'Sabha', kind: 'BSS', candidates: 0, attendance: 0.92, occurrences: 4 },
          ],
        },
        {
          name: 'Borivali-3',
          type: 'Kshetra',
          candidates: 5,
          attendance: 0.68,
          children: [
            { name: 'Yuvak Sabha', type: 'Sabha', kind: 'Regular', candidates: 2, attendance: 0.69, occurrences: 18 },
            { name: 'Baal Sabha', type: 'Sabha', kind: 'Regular', candidates: 2, attendance: 0.72, occurrences: 18 },
            { name: 'Sanyukta Sabha', type: 'Sabha', kind: 'Regular', candidates: 1, attendance: 0.61, occurrences: 18 },
          ],
        },
        {
          name: 'Vile Parle-1',
          type: 'Kshetra',
          candidates: 3,
          attendance: 0.76,
          children: [
            { name: 'Yuvak Sabha', type: 'Sabha', kind: 'Regular', candidates: 1, attendance: 0.80, occurrences: 18 },
            { name: 'Sanyukta Sabha', type: 'Sabha', kind: 'Regular', candidates: 2, attendance: 0.74, occurrences: 18 },
          ],
        },
      ],
    },
  ];

  // 8-week time series for sparklines (attendance rate %)
  const weeklyRate = [78, 76, 80, 73, 71, 69, 68, 65];
  const weeks = ['W14', 'W15', 'W16', 'W17', 'W18', 'W19', 'W20', 'W21'];

  return { roster, sabha, occurrence, sync, dedupMatch, tree, weeklyRate, weeks };
})();
