// @flow
import { makeABTest } from 'common/modules/commercial/contributions-utilities';
import { keywordExists } from 'lib/page';
import { cambridgeCopy } from 'common/modules/commercial/acquisitions-copy';

const abTestName = 'AcquisitionsEpicCambridgeAnalyticaBespokeCopy';

export const acquisitionsEpicCambridgeAnalyticaBespokeCopy: EpicABTest = makeABTest(
    {
        id: abTestName,
        campaignId: abTestName,

        start: '2018-03-23',
        expiry: '2018-04-10',

        author: 'Jonathan Rankin',
        description:
            'A test which tries custom copy on Cambridge analytica stories',
        successMeasure: 'Conversion rate',
        idealOutcome:
            'We learn the impact of moment-lead messaging on epic conversion',

        audienceCriteria: 'All',
        audience: 1,
        audienceOffset: 0,
        canRun: () => keywordExists(['Cambridge Analytica']),

        variants: [
            {
                id: 'control',
                products: [],
                options: {
                    isUnlimited: true,
                },
            },
            {
                id: 'bespoke_copy',
                products: [],
                options: {
                    copy: cambridgeCopy,
                    testimonialBlock: '',
                    isUnlimited: true,
                },
            },
        ],
    }
);